package com.example.jobico.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;

@Primary
@Service
@ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    void requireBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Set aws.s3.bucket (env S3_BUCKET) when aws.s3.enabled=true");
        }
    }

    @Override
    public String store(byte[] pdfBytes, String folder, String filename) {
        String normalizedFolder = folder.replaceAll("^/+|/+$", "");
        String safeFilename = sanitize(filename);
        final String provisionalKey = normalizedFolder + "/" + safeFilename;
        String putKey = provisionalKey;
        try {
            s3Client.headObject(b -> b.bucket(bucket).key(provisionalKey));
            String base = safeFilename.replace(".pdf", "");
            String altName = base + "_" + System.nanoTime() + ".pdf";
            putKey = normalizedFolder + "/" + altName;
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw new RuntimeException("S3 headObject failed for key: " + provisionalKey, e);
            }
        }

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(putKey)
                .contentType("application/pdf")
                .build();
        s3Client.putObject(req, RequestBody.fromBytes(pdfBytes));
        log.info("PDF stored in S3: {}", putKey);
        return putKey;
    }

    @Override
    public byte[] load(String fileKey) {
        String key = normalizeKey(fileKey);
        GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
        try {
            return s3Client.getObjectAsBytes(req).asByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF from S3: " + key, e);
        }
    }

    @Override
    public void delete(String fileKey) {
        String key = normalizeKey(fileKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("PDF deleted from S3: {}", key);
        } catch (Exception e) {
            log.error("Could not delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private String normalizeKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new RuntimeException("Document key is empty");
        }
        String k = fileKey.replace('\\', '/').trim();
        if (k.startsWith("/")) {
            k = k.substring(1);
        }
        Path asPath = Path.of(k).normalize();
        if (asPath.isAbsolute()) {
            throw new SecurityException("Illegal document key for S3: " + fileKey);
        }
        return asPath.toString().replace('\\', '/');
    }

    private static String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
