package com.lastkey.backend.ai.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ai.ocr")
public class OcrProperties {

    /*
     * Directory containing traineddata files.
     *
     * Windows example:
     * C:/Program Files/Tesseract-OCR/tessdata
     *
     * Linux example:
     * /usr/share/tesseract-ocr/5/tessdata
     */
    private String dataPath;

    /*
     * OCR language.
     */
    private String language = "eng";

    /*
     * Maximum number of scanned PDF pages to OCR.
     */
    private int maximumPdfPages = 20;

    /*
     * PDF rendering quality for OCR.
     */
    private int renderDpi = 200;

    /*
     * Minimum existing PDF text length before OCR fallback.
     */
    private int minimumEmbeddedTextLength = 40;

    /*
     * Maximum accepted upload size.
     */
    private long maximumFileSizeBytes =
            15L * 1024L * 1024L;
}