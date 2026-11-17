package com.hospital.assistant.day5.dto;

public record PdfUploadResponse5(
        String fileName,
        int pagesRead,
        int chunksStored,
        String status
) {}