package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UploadResponse {
    private String fileName;
    private String message;
    private long fileSize;

    public UploadResponse(String fileName, String message, long fileSize) {
        this.fileName = fileName;
        this.message = message;
        this.fileSize = fileSize;
    }
}
