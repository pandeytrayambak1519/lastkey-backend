package com.lastkey.backend.ai.classification.service.impl;

import com.lastkey.backend.ai.classification.dto.DocumentClassificationRequest;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;
import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import com.lastkey.backend.ai.classification.service.DocumentClassificationService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DocumentClassificationServiceImpl
        implements DocumentClassificationService {

    private static final Pattern NON_TEXT_PATTERN =
            Pattern.compile("[^a-z0-9\\s]");

    private static final Map<AiDocumentType, String>
            CATEGORY_NAMES = createCategoryNames();

    private static final Map<AiDocumentType, Map<String, Integer>>
            KEYWORD_WEIGHTS = createKeywordWeights();

    private static final int MINIMUM_CONFIDENT_SCORE = 3;

    @Override
    public DocumentClassificationResponse classify(
            DocumentClassificationRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Document classification details are required"
            );
        }

        String normalizedFileName =
                normalizeText(request.getFileName());

        String normalizedTitle =
                normalizeText(request.getTitle());

        String normalizedDescription =
                normalizeText(request.getDescription());

        String normalizedExtractedText =
                normalizeText(request.getExtractedText());

        if (normalizedFileName.isBlank()
                && normalizedTitle.isBlank()
                && normalizedDescription.isBlank()
                && normalizedExtractedText.isBlank()) {

            throw new IllegalArgumentException(
                    "At least one document detail is required for classification"
            );
        }

        Map<AiDocumentType, Integer> scores =
                initializeScores();

        Map<AiDocumentType, Set<String>> matchedKeywords =
                initializeMatchedKeywords();

        /*
         * Filename and title receive higher importance because they
         * usually describe the document more accurately.
         */
        calculateScores(
                normalizedFileName,
                3,
                scores,
                matchedKeywords
        );

        calculateScores(
                normalizedTitle,
                3,
                scores,
                matchedKeywords
        );

        calculateScores(
                normalizedDescription,
                2,
                scores,
                matchedKeywords
        );

        calculateScores(
                normalizedExtractedText,
                1,
                scores,
                matchedKeywords
        );

        AiDocumentType predictedType =
                findHighestScoringType(scores);

        int highestScore =
                scores.getOrDefault(
                        predictedType,
                        0
                );

        int secondHighestScore =
                findSecondHighestScore(
                        scores,
                        predictedType
                );

        if (highestScore < MINIMUM_CONFIDENT_SCORE) {
            predictedType = AiDocumentType.OTHER;
        }

        double confidence =
                calculateConfidence(
                        predictedType,
                        highestScore,
                        secondHighestScore,
                        scores
                );

        List<String> finalMatchedKeywords =
                matchedKeywords
                        .getOrDefault(
                                predictedType,
                                Collections.emptySet()
                        )
                        .stream()
                        .sorted()
                        .toList();

        boolean manualReviewRecommended =
                predictedType == AiDocumentType.OTHER
                        || confidence < 60.0
                        || highestScore == secondHighestScore;

        return DocumentClassificationResponse.builder()
                .predictedType(predictedType)
                .suggestedCategoryName(
                        CATEGORY_NAMES.get(
                                predictedType
                        )
                )
                .confidence(
                        roundToTwoDecimalPlaces(
                                confidence
                        )
                )
                .matchedKeywords(
                        finalMatchedKeywords
                )
                .categoryScores(
                        convertScoresToResponse(
                                scores
                        )
                )
                .manualReviewRecommended(
                        manualReviewRecommended
                )
                .explanation(
                        createExplanation(
                                predictedType,
                                confidence,
                                finalMatchedKeywords,
                                manualReviewRecommended
                        )
                )
                .build();
    }

    private void calculateScores(
            String text,
            int sourceMultiplier,
            Map<AiDocumentType, Integer> scores,
            Map<AiDocumentType, Set<String>> matchedKeywords
    ) {

        if (text == null || text.isBlank()) {
            return;
        }

        KEYWORD_WEIGHTS.forEach(
                (documentType, keywords) -> {

                    keywords.forEach(
                            (keyword, keywordWeight) -> {

                                if (containsKeyword(
                                        text,
                                        keyword
                                )) {

                                    int calculatedScore =
                                            keywordWeight
                                                    * sourceMultiplier;

                                    scores.compute(
                                            documentType,
                                            (key, existingScore) ->
                                                    existingScore == null
                                                            ? calculatedScore
                                                            : existingScore
                                                            + calculatedScore
                                    );

                                    matchedKeywords
                                            .get(documentType)
                                            .add(keyword);
                                }
                            }
                    );
                }
        );
    }

    private boolean containsKeyword(
            String text,
            String keyword
    ) {

        if (text.equals(keyword)) {
            return true;
        }

        if (text.contains(
                " " + keyword + " "
        )) {
            return true;
        }

        if (text.startsWith(
                keyword + " "
        )) {
            return true;
        }

        if (text.endsWith(
                " " + keyword
        )) {
            return true;
        }

        /*
         * Allows multi-word phrases and filename-like values.
         */
        return keyword.contains(" ")
                && text.contains(keyword);
    }

    private AiDocumentType findHighestScoringType(
            Map<AiDocumentType, Integer> scores
    ) {

        return scores.entrySet()
                .stream()
                .filter(
                        entry ->
                                entry.getKey()
                                        != AiDocumentType.OTHER
                )
                .max(
                        Map.Entry.comparingByValue()
                )
                .map(Map.Entry::getKey)
                .orElse(AiDocumentType.OTHER);
    }

    private int findSecondHighestScore(
            Map<AiDocumentType, Integer> scores,
            AiDocumentType highestType
    ) {

        return scores.entrySet()
                .stream()
                .filter(
                        entry ->
                                entry.getKey()
                                        != highestType
                )
                .filter(
                        entry ->
                                entry.getKey()
                                        != AiDocumentType.OTHER
                )
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);
    }

    private double calculateConfidence(
            AiDocumentType predictedType,
            int highestScore,
            int secondHighestScore,
            Map<AiDocumentType, Integer> scores
    ) {

        if (predictedType == AiDocumentType.OTHER
                || highestScore <= 0) {

            return 25.0;
        }

        int totalPositiveScore =
                scores.entrySet()
                        .stream()
                        .filter(
                                entry ->
                                        entry.getKey()
                                                != AiDocumentType.OTHER
                        )
                        .mapToInt(Map.Entry::getValue)
                        .filter(score -> score > 0)
                        .sum();

        if (totalPositiveScore == 0) {
            return 25.0;
        }

        double scoreShare =
                ((double) highestScore
                        / totalPositiveScore)
                        * 100.0;

        double differenceBonus =
                Math.min(
                        Math.max(
                                highestScore
                                        - secondHighestScore,
                                0
                        ) * 2.5,
                        20.0
                );

        double confidence =
                scoreShare + differenceBonus;

        return Math.min(
                Math.max(
                        confidence,
                        30.0
                ),
                98.0
        );
    }

    private String createExplanation(
            AiDocumentType predictedType,
            double confidence,
            List<String> matchedKeywords,
            boolean manualReviewRecommended
    ) {

        if (predictedType == AiDocumentType.OTHER) {

            return "The system could not find enough category-specific "
                    + "keywords. Manual category selection is recommended.";
        }

        String keywordText =
                matchedKeywords.isEmpty()
                        ? "document metadata"
                        : String.join(
                                ", ",
                                matchedKeywords
                        );

        String explanation =
                "The document was classified as "
                        + CATEGORY_NAMES.get(predictedType)
                        + " using matched indicators: "
                        + keywordText
                        + ". Confidence: "
                        + roundToTwoDecimalPlaces(confidence)
                        + "%.";

        if (manualReviewRecommended) {

            explanation +=
                    " Manual review is recommended because "
                            + "the confidence is not sufficiently high.";
        }

        return explanation;
    }

    private String normalizeText(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFKD
                )
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('.', ' ');

        normalized =
                NON_TEXT_PATTERN
                        .matcher(normalized)
                        .replaceAll(" ");

        return normalized
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<AiDocumentType, Integer>
    initializeScores() {

        Map<AiDocumentType, Integer> scores =
                new EnumMap<>(
                        AiDocumentType.class
                );

        for (AiDocumentType type
                : AiDocumentType.values()) {

            scores.put(type, 0);
        }

        return scores;
    }

    private Map<AiDocumentType, Set<String>>
    initializeMatchedKeywords() {

        Map<AiDocumentType, Set<String>> result =
                new EnumMap<>(
                        AiDocumentType.class
                );

        for (AiDocumentType type
                : AiDocumentType.values()) {

            result.put(
                    type,
                    new LinkedHashSet<>()
            );
        }

        return result;
    }

    private Map<String, Integer>
    convertScoresToResponse(
            Map<AiDocumentType, Integer> scores
    ) {

        Map<String, Integer> response =
                new LinkedHashMap<>();

        Arrays.stream(
                        AiDocumentType.values()
                )
                .filter(
                        type ->
                                type != AiDocumentType.OTHER
                )
                .sorted(
                        Comparator.comparingInt(
                                (AiDocumentType type) ->
                                        scores.getOrDefault(
                                                type,
                                                0
                                        )
                        ).reversed()
                )
                .forEach(
                        type ->
                                response.put(
                                        type.name(),
                                        scores.getOrDefault(
                                                type,
                                                0
                                        )
                                )
                );

        return response;
    }

    private double roundToTwoDecimalPlaces(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private static Map<AiDocumentType, String>
    createCategoryNames() {

        Map<AiDocumentType, String> categoryNames =
                new EnumMap<>(
                        AiDocumentType.class
                );

        categoryNames.put(
                AiDocumentType.IDENTITY,
                "Identity Documents"
        );

        categoryNames.put(
                AiDocumentType.BANKING,
                "Banking Documents"
        );

        categoryNames.put(
                AiDocumentType.INSURANCE,
                "Insurance Documents"
        );

        categoryNames.put(
                AiDocumentType.PROPERTY,
                "Property Documents"
        );

        categoryNames.put(
                AiDocumentType.MEDICAL,
                "Medical Documents"
        );

        categoryNames.put(
                AiDocumentType.EDUCATION,
                "Education Documents"
        );

        categoryNames.put(
                AiDocumentType.TAX,
                "Tax Documents"
        );

        categoryNames.put(
                AiDocumentType.LEGAL,
                "Legal Documents"
        );

        categoryNames.put(
                AiDocumentType.EMPLOYMENT,
                "Employment Documents"
        );

        categoryNames.put(
                AiDocumentType.VEHICLE,
                "Vehicle Documents"
        );

        categoryNames.put(
                AiDocumentType.INVESTMENT,
                "Investment Documents"
        );

        categoryNames.put(
                AiDocumentType.UTILITY,
                "Utility Documents"
        );

        categoryNames.put(
                AiDocumentType.OTHER,
                "Other"
        );

        return Collections.unmodifiableMap(
                categoryNames
        );
    }

    private static Map<AiDocumentType, Map<String, Integer>>
    createKeywordWeights() {

        Map<AiDocumentType, Map<String, Integer>> keywordMap =
                new EnumMap<>(
                        AiDocumentType.class
                );

        keywordMap.put(
                AiDocumentType.IDENTITY,
                keywords(
                        "aadhaar", 5,
                        "aadhar", 5,
                        "passport", 5,
                        "pan card", 5,
                        "driving licence", 5,
                        "driving license", 5,
                        "voter id", 5,
                        "identity card", 4,
                        "date of birth", 2,
                        "nationality", 2,
                        "government of india", 2,
                        "unique identification", 4
                )
        );

        keywordMap.put(
                AiDocumentType.BANKING,
                keywords(
                        "bank statement", 5,
                        "account statement", 5,
                        "bank account", 4,
                        "ifsc", 4,
                        "account number", 4,
                        "fixed deposit", 4,
                        "passbook", 5,
                        "transaction", 2,
                        "debit", 2,
                        "credit", 2,
                        "balance", 2,
                        "loan statement", 4
                )
        );

        keywordMap.put(
                AiDocumentType.INSURANCE,
                keywords(
                        "insurance", 5,
                        "policy number", 5,
                        "policy holder", 4,
                        "sum insured", 4,
                        "premium", 3,
                        "claim", 3,
                        "coverage", 3,
                        "life insurance", 5,
                        "health insurance", 5,
                        "term plan", 5,
                        "policy schedule", 4
                )
        );

        keywordMap.put(
                AiDocumentType.PROPERTY,
                keywords(
                        "property", 5,
                        "sale deed", 5,
                        "title deed", 5,
                        "registry", 5,
                        "land record", 5,
                        "lease agreement", 4,
                        "mutation", 4,
                        "plot number", 3,
                        "house document", 4,
                        "ownership", 3,
                        "real estate", 4,
                        "khata", 4
                )
        );

        keywordMap.put(
                AiDocumentType.MEDICAL,
                keywords(
                        "medical", 5,
                        "hospital", 4,
                        "doctor", 3,
                        "patient", 4,
                        "prescription", 5,
                        "diagnosis", 4,
                        "laboratory", 4,
                        "blood test", 5,
                        "medical report", 5,
                        "health report", 5,
                        "medicine", 3,
                        "discharge summary", 5
                )
        );

        keywordMap.put(
                AiDocumentType.EDUCATION,
                keywords(
                        "marksheet", 5,
                        "mark sheet", 5,
                        "degree", 5,
                        "certificate", 3,
                        "university", 4,
                        "college", 3,
                        "school", 3,
                        "semester", 3,
                        "transcript", 5,
                        "diploma", 5,
                        "graduation", 4,
                        "academic", 4
                )
        );

        keywordMap.put(
                AiDocumentType.TAX,
                keywords(
                        "income tax", 5,
                        "itr", 5,
                        "form 16", 5,
                        "tax return", 5,
                        "assessment year", 4,
                        "gst", 4,
                        "tds", 4,
                        "tax invoice", 4,
                        "taxpayer", 3,
                        "refund", 2,
                        "challan", 3
                )
        );

        keywordMap.put(
                AiDocumentType.LEGAL,
                keywords(
                        "agreement", 4,
                        "affidavit", 5,
                        "court", 5,
                        "legal notice", 5,
                        "will", 5,
                        "testament", 5,
                        "power of attorney", 5,
                        "notary", 4,
                        "petition", 4,
                        "declaration", 3,
                        "contract", 4
                )
        );

        keywordMap.put(
                AiDocumentType.EMPLOYMENT,
                keywords(
                        "offer letter", 5,
                        "appointment letter", 5,
                        "salary slip", 5,
                        "payslip", 5,
                        "experience letter", 5,
                        "relieving letter", 5,
                        "employment", 4,
                        "employee", 3,
                        "employer", 3,
                        "joining letter", 5,
                        "human resources", 3
                )
        );

        keywordMap.put(
                AiDocumentType.VEHICLE,
                keywords(
                        "vehicle", 5,
                        "registration certificate", 5,
                        "rc book", 5,
                        "chassis number", 4,
                        "engine number", 4,
                        "pollution certificate", 5,
                        "vehicle insurance", 5,
                        "driving permit", 4,
                        "registration number", 3
                )
        );

        keywordMap.put(
                AiDocumentType.INVESTMENT,
                keywords(
                        "investment", 5,
                        "mutual fund", 5,
                        "demat", 5,
                        "share certificate", 5,
                        "stock", 3,
                        "portfolio", 4,
                        "bond", 4,
                        "securities", 4,
                        "sip", 4,
                        "capital gain", 3
                )
        );

        keywordMap.put(
                AiDocumentType.UTILITY,
                keywords(
                        "electricity bill", 5,
                        "water bill", 5,
                        "gas bill", 5,
                        "telephone bill", 5,
                        "internet bill", 5,
                        "utility bill", 5,
                        "consumer number", 4,
                        "meter number", 4,
                        "billing period", 3
                )
        );

        keywordMap.put(
                AiDocumentType.OTHER,
                Collections.emptyMap()
        );

        return Collections.unmodifiableMap(
                keywordMap
        );
    }

    private static Map<String, Integer> keywords(
            Object... values
    ) {

        if (values.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Keyword configuration must contain keyword-score pairs"
            );
        }

        Map<String, Integer> result =
                new LinkedHashMap<>();

        for (int index = 0;
             index < values.length;
             index += 2) {

            String keyword =
                    values[index]
                            .toString()
                            .toLowerCase(Locale.ROOT);

            Integer weight =
                    Integer.parseInt(
                            values[index + 1]
                                    .toString()
                    );

            result.put(
                    keyword,
                    weight
            );
        }

        return Collections.unmodifiableMap(
                result
        );
    }
}