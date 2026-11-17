package com.hospital.assistant.day5.service;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.hospital.assistant.day5.dto.PdfUploadResponse5;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PdfIngestionService5 {

    private static final int MIN_TEXT_CHARS_PER_PAGE = 30;

    private final VectorStore vectorStore;
    private final AtomicReference<String> lastUploadedFileName = new AtomicReference<>();

    public PdfIngestionService5(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public PdfUploadResponse5 ingestPdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded PDF file cannot be empty.");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-document.pdf";
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only .pdf files are supported for ingestion.");
        }

        byte[] pdfBytes = file.getBytes();

        // 1. Primary pass: extract embedded text page-by-page using PagePdfDocumentReader
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                new ByteArrayResource(pdfBytes),
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build()
        );
        List<Document> rawPageDocs = pdfReader.get();

        // 2. Hybrid pass: RapidOCR fallback on any scanned/image page (< 30 text chars)
        List<Document> processedPages = extractWithRapidOcrFallback(pdfBytes, rawPageDocs, fileName);

        // 3. Split pages into overlapping token chunks
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .build();
        List<Document> chunks = textSplitter.apply(processedPages);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to extract readable text or OCR content from '" + fileName + "'."
            );
        }

        // 4. Generate embeddings (nomic-embed-text) and save into pgvector (port 5434)
        this.vectorStore.add(chunks);

        // 5. Track the most recently uploaded PDF
        this.lastUploadedFileName.set(fileName);

        return new PdfUploadResponse5(
                fileName,
                processedPages.size(),
                chunks.size(),
                "EMBEDDED_AND_STORED"
        );
    }

    private List<Document> extractWithRapidOcrFallback(byte[] pdfBytes, List<Document> rawPageDocs, String fileName) throws IOException {
        List<Document> finalDocuments = new ArrayList<>();
        InferenceEngine ocrEngine = null;

        try (PDDocument pdDocument = Loader.loadPDF(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            int totalPages = pdDocument.getNumberOfPages();

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                String extractedText = (pageIndex < rawPageDocs.size() && rawPageDocs.get(pageIndex).getText() != null)
                        ? rawPageDocs.get(pageIndex).getText().trim()
                        : "";

                String extractionMode = "PDF_TEXT";

                // Fallback to RapidOCR if page has little or no embedded text
                if (extractedText.length() < MIN_TEXT_CHARS_PER_PAGE) {
                    if (ocrEngine == null) {
                        ocrEngine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
                    }
                    BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);
                    Path tempImg = Files.createTempFile("rapidocr-page-", ".png");
                    try {
                        ImageIO.write(pageImage, "png", tempImg.toFile());
                        OcrResult ocrResult = ocrEngine.runOcr(tempImg.toAbsolutePath().toString());
                        extractedText = ocrResult.getStrRes() != null ? ocrResult.getStrRes().trim() : "";
                        extractionMode = "RAPID_OCR";
                    } finally {
                        Files.deleteIfExists(tempImg);
                    }
                }

                if (!extractedText.isBlank()) {
                    finalDocuments.add(new Document(
                            extractedText,
                            Map.of(
                                    "source_file", fileName,
                                    "page_number", pageIndex + 1,
                                    "extraction_mode", extractionMode
                            )
                    ));
                }
            }
        }

        return finalDocuments;
    }

    public String getLastUploadedFileName() {
        return this.lastUploadedFileName.get();
    }
}