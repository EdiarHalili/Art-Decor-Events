package com.artdecor.workforce.application.reports;

public record ExportFile(
        String filename,
        String contentType,
        byte[] content
) {
}
