package com.lastkey.backend.ai.ocr.service.impl;

import com.lastkey.backend.ai.ocr.config.OcrProperties;
import com.lastkey.backend.ai.ocr.dto.DocumentOcrResponse;
import com.lastkey.backend.ai.ocr.service.DocumentOcrService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentOcrServiceImpl
        implements DocumentOcrService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/bmp",
                    "image/tiff",
                    "image/webp"
            );

    private static final String PDF_MIME_TYPE =
            "application/pdf";

    private final OcrProperties ocrProperties;

    public DocumentOcrServiceImpl(
            OcrProperties ocrProperties
    ) {
        this.ocrProperties = ocrProperties;
    }

    @Override
    public DocumentOcrResponse extractText(
            MultipartFile file
    ) {

        validateFile(file);

        String mimeType =
                resolveMimeType(file);

        try {

            if (PDF_MIME_TYPE.equals(mimeType)) {

                return extractFromPdf(
                        file,
                        mimeType
                );
            }

            if (SUPPORTED_IMAGE_TYPES.contains(mimeType)) {

                return extractFromImage(
                        file,
                        mimeType
                );
            }

            throw new IllegalArgumentException(
                    "Unsupported file type. Only PDF, JPG, JPEG, "
                            + "PNG, BMP, TIFF and WEBP files are supported"
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to read the uploaded document",
                    exception
            );
        }
    }

    private DocumentOcrResponse extractFromPdf(
            MultipartFile file,
            String mimeType
    ) throws IOException {

        byte[] fileBytes =
                file.getBytes();

        try (PDDocument document =
                     Loader.loadPDF(fileBytes)) {

            validatePdfPageCount(document);

            String embeddedText =
                    extractEmbeddedPdfText(
                            document
                    );

            if (hasEnoughEmbeddedText(
                    embeddedText
            )) {

                String normalizedText =
                        normalizeExtractedText(
                                embeddedText
                        );

                return buildResponse(
                        file,
                        mimeType,
                        "PDF_TEXT",
                        normalizedText,
                        document.getNumberOfPages(),
                        calculateEmbeddedTextConfidence(
                                normalizedText
                        ),
                        "Text was extracted directly from the PDF"
                );
            }

            return performPdfOcr(
                    file,
                    mimeType,
                    document
            );
        }
    }

    private DocumentOcrResponse performPdfOcr(
            MultipartFile file,
            String mimeType,
            PDDocument document
    ) {

        ITesseract tesseract =
                createTesseract();

        PDFRenderer renderer =
                new PDFRenderer(document);

        int pageCount =
                Math.min(
                        document.getNumberOfPages(),
                        ocrProperties.getMaximumPdfPages()
                );

        StringBuilder extractedText =
                new StringBuilder();

        int successfulPages = 0;

        for (int pageIndex = 0;
             pageIndex < pageCount;
             pageIndex++) {

            try {

                BufferedImage pageImage =
                        renderer.renderImageWithDPI(
                                pageIndex,
                                ocrProperties.getRenderDpi(),
                                ImageType.RGB
                        );

                String pageText =
                        tesseract.doOCR(pageImage);

                String normalizedPageText =
                        normalizeExtractedText(
                                pageText
                        );

                if (!normalizedPageText.isBlank()) {

                    if (!extractedText.isEmpty()) {
                        extractedText.append("\n\n");
                    }

                    extractedText
                            .append("Page ")
                            .append(pageIndex + 1)
                            .append("\n")
                            .append(normalizedPageText);

                    successfulPages++;
                }

            } catch (IOException
                     | TesseractException exception) {

                /*
                 * Continue processing the remaining pages.
                 * One unreadable page should not fail the entire PDF.
                 */
            }
        }

        String finalText =
                normalizeExtractedText(
                        extractedText.toString()
                );

        validateExtractedText(
                finalText
        );

        double confidence =
                calculateOcrConfidence(
                        finalText,
                        pageCount,
                        successfulPages
                );

        return buildResponse(
                file,
                mimeType,
                "PDF_OCR",
                finalText,
                pageCount,
                confidence,
                successfulPages
                        + " of "
                        + pageCount
                        + " PDF pages were processed successfully"
        );
    }

    private DocumentOcrResponse extractFromImage(
            MultipartFile file,
            String mimeType
    ) throws IOException {

        BufferedImage image =
                ImageIO.read(
                        new ByteArrayInputStream(
                                file.getBytes()
                        )
                );

        if (image == null) {

            throw new IllegalArgumentException(
                    "The uploaded image is invalid or unreadable"
            );
        }

        ITesseract tesseract =
                createTesseract();

        try {

            String extractedText =
                    normalizeExtractedText(
                            tesseract.doOCR(image)
                    );

            validateExtractedText(
                    extractedText
            );

            double confidence =
                    calculateImageOcrConfidence(
                            extractedText
                    );

            return buildResponse(
                    file,
                    mimeType,
                    "IMAGE_OCR",
                    extractedText,
                    1,
                    confidence,
                    "Image OCR completed successfully"
            );

        } catch (TesseractException exception) {

            throw new IllegalStateException(
                    "OCR processing failed for the uploaded image",
                    exception
            );
        }
    }

    private String extractEmbeddedPdfText(
            PDDocument document
    ) throws IOException {

        PDFTextStripper textStripper =
                new PDFTextStripper();

        textStripper.setSortByPosition(true);

        return textStripper.getText(
                document
        );
    }

    private ITesseract createTesseract() {

        String dataPath =
                ocrProperties.getDataPath();

        if (dataPath == null
                || dataPath.isBlank()) {

            throw new IllegalStateException(
                    "Tesseract tessdata path is not configured"
            );
        }

        Tesseract tesseract =
                new Tesseract();

        tesseract.setDatapath(
                dataPath.trim()
        );

        tesseract.setLanguage(
                normalizeLanguage(
                        ocrProperties.getLanguage()
                )
        );

        /*
         * Page segmentation mode 3:
         * Fully automatic page segmentation.
         */
        tesseract.setPageSegMode(3);

        /*
         * OCR engine mode 1:
         * Uses the LSTM OCR engine.
         */
        tesseract.setOcrEngineMode(1);

        return tesseract;
    }

    private DocumentOcrResponse buildResponse(
            MultipartFile file,
            String mimeType,
            String extractionMethod,
            String extractedText,
            int processedPages,
            double confidence,
            String successMessage
    ) {

        boolean manualReviewRecommended =
                confidence < 65.0
                        || extractedText.length() < 50;

        String message =
                manualReviewRecommended
                        ? successMessage
                        + ". Manual review is recommended because "
                        + "the extracted text quality may be low"
                        : successMessage;

        return DocumentOcrResponse.builder()
                .originalFileName(
                        getOriginalFileName(file)
                )
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .extractionMethod(extractionMethod)
                .extractedText(extractedText)
                .extractedCharacterCount(
                        extractedText.length()
                )
                .processedPages(processedPages)
                .confidence(
                        round(confidence)
                )
                .manualReviewRecommended(
                        manualReviewRecommended
                )
                .message(message)
                .build();
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Document file is required"
            );
        }

        if (file.getSize() <= 0) {

            throw new IllegalArgumentException(
                    "Uploaded document is empty"
            );
        }

        if (file.getSize()
                > ocrProperties.getMaximumFileSizeBytes()) {

            throw new IllegalArgumentException(
                    "Document size cannot exceed "
                            + convertBytesToMegabytes(
                            ocrProperties
                                    .getMaximumFileSizeBytes()
                    )
                            + " MB"
            );
        }

        getOriginalFileName(file);
    }

    private void validatePdfPageCount(
            PDDocument document
    ) {

        if (document.getNumberOfPages() == 0) {

            throw new IllegalArgumentException(
                    "The uploaded PDF does not contain any pages"
            );
        }

        if (document.getNumberOfPages()
                > ocrProperties.getMaximumPdfPages()) {

            throw new IllegalArgumentException(
                    "PDF cannot contain more than "
                            + ocrProperties.getMaximumPdfPages()
                            + " pages"
            );
        }
    }

    private void validateExtractedText(
            String extractedText
    ) {

        if (extractedText == null
                || extractedText.isBlank()) {

            throw new IllegalStateException(
                    "No readable text was found in the document"
            );
        }
    }

    private boolean hasEnoughEmbeddedText(
            String text
    ) {

        if (text == null) {
            return false;
        }

        String normalized =
                normalizeExtractedText(text);

        return normalized.length()
                >= ocrProperties
                .getMinimumEmbeddedTextLength();
    }

    private String resolveMimeType(
            MultipartFile file
    ) {

        String contentType =
                file.getContentType();

        if (contentType != null
                && !contentType.isBlank()) {

            String normalizedType =
                    contentType
                            .trim()
                            .toLowerCase(Locale.ROOT);

            if (PDF_MIME_TYPE.equals(normalizedType)
                    || SUPPORTED_IMAGE_TYPES
                    .contains(normalizedType)) {

                return normalizedType;
            }
        }

        String fileName =
                getOriginalFileName(file)
                        .toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".pdf")) {
            return PDF_MIME_TYPE;
        }

        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")) {

            return "image/jpeg";
        }

        if (fileName.endsWith(".png")) {
            return "image/png";
        }

        if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        }

        if (fileName.endsWith(".tif")
                || fileName.endsWith(".tiff")) {

            return "image/tiff";
        }

        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }

        return "application/octet-stream";
    }

    private String getOriginalFileName(
            MultipartFile file
    ) {

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Original file name is missing"
            );
        }

        return originalFileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String normalizeExtractedText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\r\\n?", "\n")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String normalizeLanguage(
            String language
    ) {

        if (language == null
                || language.isBlank()) {

            return "eng";
        }

        return language.trim();
    }

    private double calculateEmbeddedTextConfidence(
            String text
    ) {

        double confidence = 75.0;

        if (text.length() >= 200) {
            confidence += 10.0;
        }

        if (text.length() >= 1000) {
            confidence += 8.0;
        }

        if (containsEnoughWords(text)) {
            confidence += 5.0;
        }

        return Math.min(
                confidence,
                98.0
        );
    }

    private double calculateImageOcrConfidence(
            String text
    ) {

        double confidence = 45.0;

        if (text.length() >= 50) {
            confidence += 12.0;
        }

        if (text.length() >= 200) {
            confidence += 15.0;
        }

        if (text.length() >= 500) {
            confidence += 10.0;
        }

        if (containsEnoughWords(text)) {
            confidence += 8.0;
        }

        return Math.min(
                confidence,
                92.0
        );
    }

    private double calculateOcrConfidence(
            String text,
            int totalPages,
            int successfulPages
    ) {

        double confidence =
                calculateImageOcrConfidence(text);

        if (totalPages > 0) {

            double pageSuccessRatio =
                    (double) successfulPages
                            / totalPages;

            confidence =
                    confidence
                            * pageSuccessRatio;
        }

        return Math.max(
                confidence,
                20.0
        );
    }

    private boolean containsEnoughWords(
            String text
    ) {

        return text.split("\\s+").length >= 10;
    }

    private long convertBytesToMegabytes(
            long bytes
    ) {

        return Math.max(
                1,
                bytes / (1024L * 1024L)
        );
    }

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}