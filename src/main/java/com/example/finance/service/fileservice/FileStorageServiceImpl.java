package com.example.finance.service.fileservice;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService{
    private final String storagePath = "src/main/resources/static/reports/";

    @Override
    public String saveFile(byte[] content, String fileName) {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            Path pathFile = path.resolve(fileName);
            Files.write(pathFile, content);

            return pathFile.toString();
        } catch (IOException e){
            throw new RuntimeException("Could not store file: " + e.getMessage());
        }
    }
}
