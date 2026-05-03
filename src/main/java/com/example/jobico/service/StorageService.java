package com.example.jobico.service;

public interface StorageService {

   
    String store(byte[] pdfBytes, String folder, String filename);

    byte[] load(String fileKey);

    void delete(String fileKey);
}