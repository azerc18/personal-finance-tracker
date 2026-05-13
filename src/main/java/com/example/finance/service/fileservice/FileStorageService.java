package com.example.finance.service.fileservice;

public interface FileStorageService {
    String saveFile(byte[] content, String fileName);
}
