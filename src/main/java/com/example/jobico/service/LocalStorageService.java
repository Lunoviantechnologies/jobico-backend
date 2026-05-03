package com.example.jobico.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Primary
@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    /** Configure in application.properties: app.document.storage-dir=uploads/documents */
    @Value("${app.document.storage-dir:uploads/documents}")
    private String storageRoot;

    // ── StorageService impl ───────────────────────────────────────────────────

    @Override
    public String store(byte[] pdfBytes, String folder, String filename) {
        try {
            Path dir = resolveDir(folder);
            String safeFilename = sanitize(filename);
            Path target = dir.resolve(safeFilename);

            // Never silently overwrite — append nanotime if clash
            if (Files.exists(target)) {
                String base = safeFilename.replace(".pdf", "");
                safeFilename = base + "_" + System.nanoTime() + ".pdf";
                target = dir.resolve(safeFilename);
            }

            Files.write(target, pdfBytes, StandardOpenOption.CREATE_NEW);
            String relativeKey = folder + "/" + safeFilename;
            log.info("PDF stored: {}", relativeKey);
            return relativeKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store PDF document: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] load(String fileKey) {
        try {
            Path path = Paths.get(storageRoot, fileKey).normalize();
            guardPathTraversal(path);
            if (!Files.exists(path)) {
                throw new RuntimeException("Document not found: " + fileKey);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF document: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileKey) {
        try {
            Path path = Paths.get(storageRoot, fileKey).normalize();
            guardPathTraversal(path);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("PDF deleted: {}", fileKey);
            } else {
                log.warn("PDF not found on disk during delete: {}", fileKey);
            }
        } catch (IOException e) {
            // Log but don't throw — DB record deletion should still succeed
            log.error("Could not delete PDF file {}: {}", fileKey, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path resolveDir(String folder) throws IOException {
        Path dir = Paths.get(storageRoot, folder).normalize();
        guardPathTraversal(dir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

   
    private void guardPathTraversal(Path path) {
        Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw new SecurityException("Illegal file path detected: " + path);
        }
    }

    private static String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}