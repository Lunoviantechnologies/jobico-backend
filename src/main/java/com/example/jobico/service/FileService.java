package com.example.jobico.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Optional<S3Client> s3Client;
    private final String uploadDir;
    private final boolean s3Enabled;
    private final String s3Bucket;
    private final String resumeS3Prefix;

    public FileService(
            @Autowired(required = false) S3Client s3Client,
            @Value("${file.upload-dir}") String uploadDir,
            @Value("${aws.s3.enabled:false}") boolean s3Enabled,
            @Value("${aws.s3.bucket:}") String s3Bucket,
            @Value("${aws.s3.resume-prefix:resumes}") String resumeS3Prefix) {
        this.s3Client = Optional.ofNullable(s3Client);
        this.uploadDir = uploadDir;
        this.s3Enabled = s3Enabled;
        this.s3Bucket = s3Bucket;
        this.resumeS3Prefix = stripSlashes(resumeS3Prefix);
    }

    private boolean useS3() {
        return s3Enabled && s3Client.isPresent() && s3Bucket != null && !s3Bucket.isBlank();
    }

    private boolean isS3ResumeKey(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        String pfx = resumeS3Prefix;
        return normalized.startsWith(pfx + "/");
    }

    private static String stripSlashes(String s) {
        if (s == null || s.isBlank()) {
            return "resumes";
        }
        return s.replaceAll("^/+|/+$", "");
    }

    public String saveResume(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Only PDF, DOC, or DOCX files are allowed");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException("File size exceeds 10MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String uniqueFilename = UUID.randomUUID() + extension;

        if (useS3()) {
            String key = resumeS3Prefix + "/" + uniqueFilename;
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            try (var in = file.getInputStream()) {
                s3Client.get().putObject(req, RequestBody.fromInputStream(in, file.getSize()));
            }
            return key;
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Files.copy(file.getInputStream(), uploadPath.resolve(uniqueFilename),
                StandardCopyOption.REPLACE_EXISTING);
        Path base = Paths.get(uploadDir).normalize();
        Path filePath = base.resolve(uniqueFilename).normalize();
        return filePath.toString();
    }

    public Resource loadResumeAsResource(String filePath) {
        if (useS3() && isS3ResumeKey(filePath)) {
            byte[] bytes = s3Client.get().getObjectAsBytes(
                    GetObjectRequest.builder().bucket(s3Bucket).key(filePath.replace('\\', '/')).build()).asByteArray();
            String normalized = filePath.replace('\\', '/');
            final String fname = normalized.contains("/")
                    ? normalized.substring(normalized.lastIndexOf('/') + 1)
                    : normalized;
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fname;
                }
            };
        }
        try {
            Path path = Paths.get(filePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("File not found or not readable: " + filePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    public void deleteResume(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        if (useS3() && isS3ResumeKey(filePath)) {
            try {
                s3Client.get().deleteObject(DeleteObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(filePath.replace('\\', '/'))
                        .build());
            } catch (Exception e) {
                System.err.println("Warning: Could not delete resume from S3: " + filePath);
            }
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Warning: Could not delete resume file: " + filePath);
        }
    }
}
