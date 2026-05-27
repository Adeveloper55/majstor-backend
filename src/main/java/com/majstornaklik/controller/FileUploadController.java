package com.majstornaklik.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fajl je prazan");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Dozvoljene su samo slike");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String ext = contentType.replace("image/", "");
        if ("jpeg".equals(ext)) ext = "jpg";
        String filename = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename);
        Files.write(target, file.getBytes());

        String url = backendUrl + "/uploads/" + filename;
        return ResponseEntity.ok(Map.of("url", url));
    }
}
