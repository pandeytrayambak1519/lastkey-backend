package com.lastkey.backend.ai.summary.service.impl;

import com.lastkey.backend.ai.classification.dto.DocumentClassificationRequest;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;
import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import com.lastkey.backend.ai.classification.service.DocumentClassificationService;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryRequest;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryResponse;
import com.lastkey.backend.ai.summary.service.AiDocumentSummaryService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiDocumentSummaryServiceImpl
        implements AiDocumentSummaryService {

    private static final Pattern DATE_PATTERN =
            Pattern.compile(
                    "\\b(?:0?[1-9]|[12][0-9]|3[01])"
                            + "[-/.]"
                            + "(?:0?[1-9]|1[0-2])"
                            + "[-/.]"
                            + "(?:19|20)?\\d{2}\\b"
            );

    private static final Pattern YEAR_PATTERN =
            Pattern.compile("\\b(?:19|20)\\d{2}\\b");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z0-9._%+-]+"
                            + "@[A-Za-z0-9.-]+"
                            + "\\.[A-Za-z]{2,}\\b"
            );

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "(?:\\+91[-\\s]?)?[6-9]\\d{9}"
            );

    private static final Map<AiDocumentType, String>
            DOCUMENT_TYPE_NAMES = createDocumentTypeNames();

    private static final Map<AiDocumentType, List<String>>
            REQUIRED_FIELDS = createRequiredFields();

    private final DocumentClassificationService
            documentClassificationService;

    public AiDocumentSummaryServiceImpl(
            DocumentClassificationService documentClassificationService
    ) {
        this.documentClassificationService =
                documentClassificationService;
    }

    @Override
    public AiDocumentSummaryResponse generateSummary(
            AiDocumentSummaryRequest request
    ) {

        validateRequest(request);

        String extractedText =
                normalizeText(request.getExtractedText());

        AiDocumentType documentType =
                resolveDocumentType(
                        request,
                        extractedText
                );

        Map<String, String> extractedFields =
                extractFields(
                        documentType,
                        extractedText
                );

        addCommonFields(
                extractedText,
                extractedFields
        );

        List<String> missingFields =
                findMissingFields(
                        documentType,
                        extractedFields
                );

        double confidence =
                calculateConfidence(
                        documentType,
                        extractedFields,
                        missingFields,
                        extractedText
                );

        boolean manualReviewRecommended =
                documentType == AiDocumentType.OTHER
                        || confidence < 60.0
                        || extractedFields.isEmpty();

        String summary =
                createSummary(
                        documentType,
                        extractedFields,
                        missingFields
                );

        String extractionMessage =
                createExtractionMessage(
                        documentType,
                        extractedFields,
                        missingFields,
                        confidence
                );

        return AiDocumentSummaryResponse.builder()
                .documentType(documentType)
                .documentTypeName(
                        DOCUMENT_TYPE_NAMES.getOrDefault(
                                documentType,
                                "Other Document"
                        )
                )
                .summary(summary)
                .extractedFields(extractedFields)
                .missingFields(missingFields)
                .confidence(round(confidence))
                .manualReviewRecommended(
                        manualReviewRecommended
                )
                .totalDetectedFields(
                        extractedFields.size()
                )
                .extractionMessage(
                        extractionMessage
                )
                .build();
    }

    private AiDocumentType resolveDocumentType(
            AiDocumentSummaryRequest request,
            String extractedText
    ) {

        if (request.getDocumentType() != null) {
            return request.getDocumentType();
        }

        DocumentClassificationRequest classificationRequest =
                DocumentClassificationRequest.builder()
                        .fileName(request.getFileName())
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .extractedText(extractedText)
                        .build();

        DocumentClassificationResponse classificationResponse =
                documentClassificationService.classify(
                        classificationRequest
                );

        if (classificationResponse.getPredictedType() == null) {
            return AiDocumentType.OTHER;
        }

        return classificationResponse.getPredictedType();
    }

    private Map<String, String> extractFields(
            AiDocumentType documentType,
            String text
    ) {

        Map<String, String> fields =
                new LinkedHashMap<>();

        switch (documentType) {

            case IDENTITY ->
                    extractIdentityFields(
                            text,
                            fields
                    );

            case BANKING ->
                    extractBankingFields(
                            text,
                            fields
                    );

            case INSURANCE ->
                    extractInsuranceFields(
                            text,
                            fields
                    );

            case PROPERTY ->
                    extractPropertyFields(
                            text,
                            fields
                    );

            case MEDICAL ->
                    extractMedicalFields(
                            text,
                            fields
                    );

            case EDUCATION ->
                    extractEducationFields(
                            text,
                            fields
                    );

            case TAX ->
                    extractTaxFields(
                            text,
                            fields
                    );

            case LEGAL ->
                    extractLegalFields(
                            text,
                            fields
                    );

            case EMPLOYMENT ->
                    extractEmploymentFields(
                            text,
                            fields
                    );

            case VEHICLE ->
                    extractVehicleFields(
                            text,
                            fields
                    );

            case INVESTMENT ->
                    extractInvestmentFields(
                            text,
                            fields
                    );

            case UTILITY ->
                    extractUtilityFields(
                            text,
                            fields
                    );

            case OTHER ->
                    extractGeneralFields(
                            text,
                            fields
                    );
        }

        return fields;
    }

    private void extractIdentityFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "name",
                extractLabelValue(
                        text,
                        "name",
                        "full name",
                        "holder name",
                        "surname"
                )
        );

        putIfPresent(
                fields,
                "dateOfBirth",
                extractDateAfterLabels(
                        text,
                        "date of birth",
                        "dob",
                        "birth date"
                )
        );

        putIfPresent(
                fields,
                "passportNumber",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z][0-9]{7}\\b"
                )
        );

        putIfPresent(
                fields,
                "panNumber",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
                )
        );

        putIfPresent(
                fields,
                "aadhaarNumber",
                extractUsingPattern(
                        text,
                        "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
                )
        );

        putIfPresent(
                fields,
                "drivingLicenceNumber",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z]{2}[\\s-]?\\d{2}"
                                + "[\\s-]?\\d{4}[\\s-]?\\d{7}\\b"
                )
        );

        putIfPresent(
                fields,
                "nationality",
                extractLabelValue(
                        text,
                        "nationality",
                        "country of citizenship",
                        "citizenship"
                )
        );

        putIfPresent(
                fields,
                "expiryDate",
                extractDateAfterLabels(
                        text,
                        "date of expiry",
                        "expiry date",
                        "valid until",
                        "valid upto"
                )
        );
    }

    private void extractBankingFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "accountHolderName",
                extractLabelValue(
                        text,
                        "account holder",
                        "customer name",
                        "account name"
                )
        );

        putIfPresent(
                fields,
                "accountNumber",
                extractUsingPattern(
                        text,
                        "(?i)(?:account|a/c)[\\s:-]*"
                                + "(?:number|no)?[\\s:-]*"
                                + "([0-9Xx*]{6,20})"
                )
        );

        putIfPresent(
                fields,
                "ifscCode",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z]{4}0[A-Z0-9]{6}\\b"
                )
        );

        putIfPresent(
                fields,
                "bankName",
                extractLabelValue(
                        text,
                        "bank name",
                        "name of bank"
                )
        );

        putIfPresent(
                fields,
                "branchName",
                extractLabelValue(
                        text,
                        "branch",
                        "branch name"
                )
        );

        putIfPresent(
                fields,
                "statementPeriod",
                extractLabelValue(
                        text,
                        "statement period",
                        "period",
                        "from date"
                )
        );

        putIfPresent(
                fields,
                "closingBalance",
                extractAmountAfterLabels(
                        text,
                        "closing balance",
                        "available balance",
                        "current balance"
                )
        );
    }

    private void extractInsuranceFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "policyNumber",
                extractUsingPattern(
                        text,
                        "(?i)(?:policy number|policy no|policy #)"
                                + "[\\s:-]*([A-Z0-9/-]{5,30})"
                )
        );

        putIfPresent(
                fields,
                "policyHolder",
                extractLabelValue(
                        text,
                        "policy holder",
                        "insured name",
                        "name of insured",
                        "proposer name"
                )
        );

        putIfPresent(
                fields,
                "insuranceCompany",
                extractLabelValue(
                        text,
                        "insurance company",
                        "insurer",
                        "company name"
                )
        );

        putIfPresent(
                fields,
                "premiumAmount",
                extractAmountAfterLabels(
                        text,
                        "premium amount",
                        "total premium",
                        "premium"
                )
        );

        putIfPresent(
                fields,
                "sumInsured",
                extractAmountAfterLabels(
                        text,
                        "sum insured",
                        "coverage amount",
                        "assured amount",
                        "sum assured"
                )
        );

        putIfPresent(
                fields,
                "nomineeName",
                extractLabelValue(
                        text,
                        "nominee name",
                        "nominee",
                        "beneficiary"
                )
        );

        putIfPresent(
                fields,
                "startDate",
                extractDateAfterLabels(
                        text,
                        "policy start date",
                        "commencement date",
                        "valid from"
                )
        );

        putIfPresent(
                fields,
                "expiryDate",
                extractDateAfterLabels(
                        text,
                        "policy expiry date",
                        "expiry date",
                        "valid until",
                        "maturity date"
                )
        );
    }

    private void extractPropertyFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "ownerName",
                extractLabelValue(
                        text,
                        "owner name",
                        "purchaser",
                        "buyer",
                        "transferee"
                )
        );

        putIfPresent(
                fields,
                "sellerName",
                extractLabelValue(
                        text,
                        "seller name",
                        "vendor",
                        "transferor"
                )
        );

        putIfPresent(
                fields,
                "registrationNumber",
                extractLabelValue(
                        text,
                        "registration number",
                        "registration no",
                        "document number"
                )
        );

        putIfPresent(
                fields,
                "propertyAddress",
                extractLabelValue(
                        text,
                        "property address",
                        "address of property",
                        "situated at"
                )
        );

        putIfPresent(
                fields,
                "plotNumber",
                extractLabelValue(
                        text,
                        "plot number",
                        "plot no",
                        "survey number",
                        "khasra number"
                )
        );

        putIfPresent(
                fields,
                "propertyArea",
                extractLabelValue(
                        text,
                        "total area",
                        "property area",
                        "built up area",
                        "carpet area"
                )
        );

        putIfPresent(
                fields,
                "registrationDate",
                extractDateAfterLabels(
                        text,
                        "registration date",
                        "registered on",
                        "date of registration"
                )
        );
    }

    private void extractMedicalFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "patientName",
                extractLabelValue(
                        text,
                        "patient name",
                        "name of patient",
                        "patient"
                )
        );

        putIfPresent(
                fields,
                "doctorName",
                extractLabelValue(
                        text,
                        "doctor name",
                        "consultant",
                        "physician",
                        "doctor"
                )
        );

        putIfPresent(
                fields,
                "hospitalName",
                extractLabelValue(
                        text,
                        "hospital name",
                        "clinic name",
                        "laboratory name"
                )
        );

        putIfPresent(
                fields,
                "reportDate",
                extractDateAfterLabels(
                        text,
                        "report date",
                        "test date",
                        "sample date",
                        "date"
                )
        );

        putIfPresent(
                fields,
                "diagnosis",
                extractLabelValue(
                        text,
                        "diagnosis",
                        "clinical diagnosis",
                        "impression"
                )
        );

        putIfPresent(
                fields,
                "prescription",
                extractLabelValue(
                        text,
                        "prescription",
                        "medication",
                        "medicine advised"
                )
        );
    }

    private void extractEducationFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "studentName",
                extractLabelValue(
                        text,
                        "student name",
                        "candidate name",
                        "name"
                )
        );

        putIfPresent(
                fields,
                "rollNumber",
                extractLabelValue(
                        text,
                        "roll number",
                        "roll no",
                        "enrollment number",
                        "registration number"
                )
        );

        putIfPresent(
                fields,
                "institutionName",
                extractLabelValue(
                        text,
                        "university",
                        "college",
                        "school",
                        "institution"
                )
        );

        putIfPresent(
                fields,
                "courseName",
                extractLabelValue(
                        text,
                        "course",
                        "programme",
                        "degree",
                        "branch"
                )
        );

        putIfPresent(
                fields,
                "passingYear",
                extractUsingPattern(
                        text,
                        "\\b(?:19|20)\\d{2}\\b"
                )
        );

        putIfPresent(
                fields,
                "percentageOrCgpa",
                extractUsingPattern(
                        text,
                        "(?i)(?:cgpa|percentage|percent|grade)"
                                + "[\\s:-]*([0-9.]{1,5}%?)"
                )
        );
    }

    private void extractTaxFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "taxpayerName",
                extractLabelValue(
                        text,
                        "taxpayer name",
                        "assessee name",
                        "name"
                )
        );

        putIfPresent(
                fields,
                "panNumber",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
                )
        );

        putIfPresent(
                fields,
                "assessmentYear",
                extractUsingPattern(
                        text,
                        "(?i)(?:assessment year|a\\.y\\.)"
                                + "[\\s:-]*(20\\d{2}[\\s-]*20\\d{2})"
                )
        );

        putIfPresent(
                fields,
                "financialYear",
                extractUsingPattern(
                        text,
                        "(?i)(?:financial year|f\\.y\\.)"
                                + "[\\s:-]*(20\\d{2}[\\s-]*20\\d{2})"
                )
        );

        putIfPresent(
                fields,
                "acknowledgementNumber",
                extractLabelValue(
                        text,
                        "acknowledgement number",
                        "acknowledgment number",
                        "receipt number"
                )
        );

        putIfPresent(
                fields,
                "totalIncome",
                extractAmountAfterLabels(
                        text,
                        "total income",
                        "gross total income",
                        "taxable income"
                )
        );
    }

    private void extractLegalFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "documentNumber",
                extractLabelValue(
                        text,
                        "document number",
                        "case number",
                        "petition number",
                        "agreement number"
                )
        );

        putIfPresent(
                fields,
                "firstParty",
                extractLabelValue(
                        text,
                        "first party",
                        "party of the first part",
                        "petitioner"
                )
        );

        putIfPresent(
                fields,
                "secondParty",
                extractLabelValue(
                        text,
                        "second party",
                        "party of the second part",
                        "respondent"
                )
        );

        putIfPresent(
                fields,
                "executionDate",
                extractDateAfterLabels(
                        text,
                        "execution date",
                        "agreement date",
                        "dated",
                        "date"
                )
        );

        putIfPresent(
                fields,
                "courtName",
                extractLabelValue(
                        text,
                        "court name",
                        "before the court",
                        "tribunal"
                )
        );
    }

    private void extractEmploymentFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "employeeName",
                extractLabelValue(
                        text,
                        "employee name",
                        "candidate name",
                        "name"
                )
        );

        putIfPresent(
                fields,
                "employeeId",
                extractLabelValue(
                        text,
                        "employee id",
                        "employee code",
                        "staff id"
                )
        );

        putIfPresent(
                fields,
                "companyName",
                extractLabelValue(
                        text,
                        "company name",
                        "employer",
                        "organization"
                )
        );

        putIfPresent(
                fields,
                "designation",
                extractLabelValue(
                        text,
                        "designation",
                        "job title",
                        "position"
                )
        );

        putIfPresent(
                fields,
                "joiningDate",
                extractDateAfterLabels(
                        text,
                        "joining date",
                        "date of joining",
                        "employment start date"
                )
        );

        putIfPresent(
                fields,
                "salary",
                extractAmountAfterLabels(
                        text,
                        "gross salary",
                        "net salary",
                        "salary",
                        "ctc"
                )
        );
    }

    private void extractVehicleFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "ownerName",
                extractLabelValue(
                        text,
                        "owner name",
                        "registered owner"
                )
        );

        putIfPresent(
                fields,
                "registrationNumber",
                extractUsingPattern(
                        text,
                        "(?i)\\b[A-Z]{2}[\\s-]?"
                                + "\\d{1,2}[\\s-]?"
                                + "[A-Z]{1,3}[\\s-]?"
                                + "\\d{1,4}\\b"
                )
        );

        putIfPresent(
                fields,
                "chassisNumber",
                extractLabelValue(
                        text,
                        "chassis number",
                        "chassis no"
                )
        );

        putIfPresent(
                fields,
                "engineNumber",
                extractLabelValue(
                        text,
                        "engine number",
                        "engine no"
                )
        );

        putIfPresent(
                fields,
                "vehicleModel",
                extractLabelValue(
                        text,
                        "model",
                        "maker model",
                        "vehicle model"
                )
        );

        putIfPresent(
                fields,
                "registrationDate",
                extractDateAfterLabels(
                        text,
                        "registration date",
                        "date of registration"
                )
        );

        putIfPresent(
                fields,
                "validUntil",
                extractDateAfterLabels(
                        text,
                        "valid until",
                        "valid upto",
                        "expiry date"
                )
        );
    }

    private void extractInvestmentFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "investorName",
                extractLabelValue(
                        text,
                        "investor name",
                        "holder name",
                        "client name"
                )
        );

        putIfPresent(
                fields,
                "folioNumber",
                extractLabelValue(
                        text,
                        "folio number",
                        "folio no",
                        "account number"
                )
        );

        putIfPresent(
                fields,
                "fundName",
                extractLabelValue(
                        text,
                        "fund name",
                        "scheme name",
                        "security name"
                )
        );

        putIfPresent(
                fields,
                "investmentAmount",
                extractAmountAfterLabels(
                        text,
                        "investment amount",
                        "invested amount",
                        "purchase value"
                )
        );

        putIfPresent(
                fields,
                "currentValue",
                extractAmountAfterLabels(
                        text,
                        "current value",
                        "market value",
                        "valuation"
                )
        );

        putIfPresent(
                fields,
                "investmentDate",
                extractDateAfterLabels(
                        text,
                        "investment date",
                        "purchase date",
                        "transaction date"
                )
        );
    }

    private void extractUtilityFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "consumerName",
                extractLabelValue(
                        text,
                        "consumer name",
                        "customer name",
                        "subscriber name"
                )
        );

        putIfPresent(
                fields,
                "consumerNumber",
                extractLabelValue(
                        text,
                        "consumer number",
                        "consumer no",
                        "customer id",
                        "account number"
                )
        );

        putIfPresent(
                fields,
                "billNumber",
                extractLabelValue(
                        text,
                        "bill number",
                        "invoice number",
                        "bill no"
                )
        );

        putIfPresent(
                fields,
                "billingPeriod",
                extractLabelValue(
                        text,
                        "billing period",
                        "bill period"
                )
        );

        putIfPresent(
                fields,
                "dueDate",
                extractDateAfterLabels(
                        text,
                        "due date",
                        "pay by",
                        "payment due date"
                )
        );

        putIfPresent(
                fields,
                "billAmount",
                extractAmountAfterLabels(
                        text,
                        "amount payable",
                        "total amount",
                        "bill amount",
                        "net payable"
                )
        );
    }

    private void extractGeneralFields(
            String text,
            Map<String, String> fields
    ) {

        putIfPresent(
                fields,
                "documentDate",
                extractFirstMatch(
                        DATE_PATTERN,
                        text
                )
        );

        putIfPresent(
                fields,
                "referenceNumber",
                extractLabelValue(
                        text,
                        "reference number",
                        "document number",
                        "serial number",
                        "registration number"
                )
        );
    }

    private void addCommonFields(
            String text,
            Map<String, String> fields
    ) {

        if (!fields.containsKey("email")) {

            putIfPresent(
                    fields,
                    "email",
                    extractFirstMatch(
                            EMAIL_PATTERN,
                            text
                    )
            );
        }

        if (!fields.containsKey("phoneNumber")) {

            putIfPresent(
                    fields,
                    "phoneNumber",
                    extractFirstMatch(
                            PHONE_PATTERN,
                            text
                    )
            );
        }

        if (!containsDateField(fields)) {

            putIfPresent(
                    fields,
                    "detectedDate",
                    extractFirstMatch(
                            DATE_PATTERN,
                            text
                    )
            );
        }

        if (!containsYearField(fields)) {

            putIfPresent(
                    fields,
                    "detectedYear",
                    extractFirstMatch(
                            YEAR_PATTERN,
                            text
                    )
            );
        }
    }

    private String extractLabelValue(
            String text,
            String... labels
    ) {

        for (String label : labels) {

            Pattern pattern =
                    Pattern.compile(
                            "(?im)^\\s*"
                                    + Pattern.quote(label)
                                    + "\\s*[:\\-]?\\s*"
                                    + "([^\\r\\n]{2,120})"
                    );

            Matcher matcher =
                    pattern.matcher(text);

            if (matcher.find()) {

                String value =
                        cleanExtractedValue(
                                matcher.group(1)
                        );

                if (isValidExtractedValue(value)) {
                    return value;
                }
            }
        }

        return null;
    }

    private String extractDateAfterLabels(
            String text,
            String... labels
    ) {

        for (String label : labels) {

            Pattern pattern =
                    Pattern.compile(
                            "(?i)"
                                    + Pattern.quote(label)
                                    + "\\s*[:\\-]?\\s*"
                                    + "((?:0?[1-9]|[12][0-9]|3[01])"
                                    + "[-/.]"
                                    + "(?:0?[1-9]|1[0-2])"
                                    + "[-/.]"
                                    + "(?:19|20)?\\d{2})"
                    );

            Matcher matcher =
                    pattern.matcher(text);

            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return null;
    }

    private String extractAmountAfterLabels(
            String text,
            String... labels
    ) {

        for (String label : labels) {

            Pattern pattern =
                    Pattern.compile(
                            "(?i)"
                                    + Pattern.quote(label)
                                    + "\\s*[:\\-]?\\s*"
                                    + "(?:rs\\.?|inr|₹)?\\s*"
                                    + "([0-9,]+(?:\\.\\d{1,2})?)"
                    );

            Matcher matcher =
                    pattern.matcher(text);

            if (matcher.find()) {
                return matcher.group(1)
                        .replace(",", "")
                        .trim();
            }
        }

        return null;
    }

    private String extractUsingPattern(
            String text,
            String regex
    ) {

        Pattern pattern =
                Pattern.compile(regex);

        Matcher matcher =
                pattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String value;

        if (matcher.groupCount() >= 1
                && matcher.group(1) != null) {

            value = matcher.group(1);

        } else {
            value = matcher.group();
        }

        return cleanExtractedValue(value);
    }

    private String extractFirstMatch(
            Pattern pattern,
            String text
    ) {

        Matcher matcher =
                pattern.matcher(text);

        if (matcher.find()) {
            return cleanExtractedValue(
                    matcher.group()
            );
        }

        return null;
    }

    private List<String> findMissingFields(
            AiDocumentType documentType,
            Map<String, String> fields
    ) {

        List<String> requiredFields =
                REQUIRED_FIELDS.getOrDefault(
                        documentType,
                        Collections.emptyList()
                );

        return requiredFields.stream()
                .filter(
                        field ->
                                !fields.containsKey(field)
                                        || fields.get(field) == null
                                        || fields.get(field).isBlank()
                )
                .toList();
    }

    private double calculateConfidence(
            AiDocumentType documentType,
            Map<String, String> fields,
            List<String> missingFields,
            String extractedText
    ) {

        if (extractedText == null
                || extractedText.length() < 20) {

            return 20.0;
        }

        List<String> requiredFields =
                REQUIRED_FIELDS.getOrDefault(
                        documentType,
                        Collections.emptyList()
                );

        double confidence = 35.0;

        confidence +=
                Math.min(
                        fields.size() * 8.0,
                        40.0
                );

        if (!requiredFields.isEmpty()) {

            int detectedRequiredFields =
                    requiredFields.size()
                            - missingFields.size();

            double requiredFieldScore =
                    ((double) detectedRequiredFields
                            / requiredFields.size())
                            * 20.0;

            confidence += requiredFieldScore;
        }

        if (extractedText.length() >= 100) {
            confidence += 3.0;
        }

        if (extractedText.length() >= 500) {
            confidence += 2.0;
        }

        if (documentType == AiDocumentType.OTHER) {
            confidence -= 15.0;
        }

        return Math.max(
                10.0,
                Math.min(
                        confidence,
                        98.0
                )
        );
    }

    private String createSummary(
            AiDocumentType documentType,
            Map<String, String> fields,
            List<String> missingFields
    ) {

        String typeName =
                DOCUMENT_TYPE_NAMES.getOrDefault(
                        documentType,
                        "Other Document"
                );

        if (fields.isEmpty()) {

            return typeName
                    + " was detected, but important information "
                    + "could not be extracted from the OCR text.";
        }

        StringBuilder summary =
                new StringBuilder();

        summary.append(typeName)
                .append(" detected. ");

        int fieldCount = 0;

        for (Map.Entry<String, String> entry
                : fields.entrySet()) {

            if (fieldCount >= 5) {
                break;
            }

            summary.append(
                            convertFieldNameToLabel(
                                    entry.getKey()
                            )
                    )
                    .append(": ")
                    .append(
                            maskSensitiveValue(
                                    entry.getKey(),
                                    entry.getValue()
                            )
                    )
                    .append(". ");

            fieldCount++;
        }

        if (!missingFields.isEmpty()) {

            summary.append("Some information could not be detected: ")
                    .append(
                            missingFields.stream()
                                    .map(this::convertFieldNameToLabel)
                                    .limit(4)
                                    .reduce(
                                            (first, second) ->
                                                    first + ", " + second
                                    )
                                    .orElse("")
                    )
                    .append(".");
        }

        return summary
                .toString()
                .trim();
    }

    private String createExtractionMessage(
            AiDocumentType documentType,
            Map<String, String> fields,
            List<String> missingFields,
            double confidence
    ) {

        if (documentType == AiDocumentType.OTHER) {

            return "The document type could not be identified accurately. "
                    + "Please review the extracted information manually.";
        }

        if (fields.isEmpty()) {

            return "No structured information was detected. "
                    + "The uploaded file may have low OCR quality.";
        }

        if (confidence < 60.0) {

            return "Information was extracted with low confidence. "
                    + "Manual verification is recommended.";
        }

        if (!missingFields.isEmpty()) {

            return "Information extraction completed, but some expected "
                    + "fields were not found.";
        }

        return "Document summary and structured information "
                + "were generated successfully.";
    }

    private String maskSensitiveValue(
            String fieldName,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return value;
        }

        String normalizedField =
                fieldName.toLowerCase(Locale.ROOT);

        boolean sensitive =
                normalizedField.contains("aadhaar")
                        || normalizedField.contains("accountnumber")
                        || normalizedField.contains("passportnumber")
                        || normalizedField.contains("pannumber")
                        || normalizedField.contains("chassisnumber")
                        || normalizedField.contains("enginenumber")
                        || normalizedField.contains("consumerNumber");

        if (!sensitive || value.length() <= 4) {
            return value;
        }

        int visibleCharacters =
                Math.min(
                        4,
                        value.length()
                );

        return "*".repeat(
                value.length()
                        - visibleCharacters
        ) + value.substring(
                value.length()
                        - visibleCharacters
        );
    }

    private String convertFieldNameToLabel(
            String fieldName
    ) {

        if (fieldName == null || fieldName.isBlank()) {
            return "";
        }

        String separated =
                fieldName.replaceAll(
                        "([a-z])([A-Z])",
                        "$1 $2"
                );

        return separated.substring(0, 1)
                .toUpperCase(Locale.ROOT)
                + separated.substring(1);
    }

    private boolean containsDateField(
            Map<String, String> fields
    ) {

        return fields.keySet()
                .stream()
                .anyMatch(
                        key ->
                                key.toLowerCase(Locale.ROOT)
                                        .contains("date")
                );
    }

    private boolean containsYearField(
            Map<String, String> fields
    ) {

        return fields.keySet()
                .stream()
                .anyMatch(
                        key ->
                                key.toLowerCase(Locale.ROOT)
                                        .contains("year")
                );
    }

    private void putIfPresent(
            Map<String, String> fields,
            String key,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        fields.put(
                key,
                value.trim()
        );
    }

    private String cleanExtractedValue(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return value
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s:;,-]+", "")
                .replaceAll("[\\s:;,-]+$", "")
                .trim();
    }

    private boolean isValidExtractedValue(
            String value
    ) {

        return value != null
                && value.length() >= 2
                && value.length() <= 120;
    }

    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void validateRequest(
            AiDocumentSummaryRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Document summary request is required"
            );
        }

        if (request.getExtractedText() == null
                || request.getExtractedText().isBlank()) {

            throw new IllegalArgumentException(
                    "Extracted document text is required"
            );
        }
    }

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private static Map<AiDocumentType, String>
    createDocumentTypeNames() {

        Map<AiDocumentType, String> names =
                new EnumMap<>(
                        AiDocumentType.class
                );

        names.put(
                AiDocumentType.IDENTITY,
                "Identity Document"
        );

        names.put(
                AiDocumentType.BANKING,
                "Banking Document"
        );

        names.put(
                AiDocumentType.INSURANCE,
                "Insurance Document"
        );

        names.put(
                AiDocumentType.PROPERTY,
                "Property Document"
        );

        names.put(
                AiDocumentType.MEDICAL,
                "Medical Document"
        );

        names.put(
                AiDocumentType.EDUCATION,
                "Education Document"
        );

        names.put(
                AiDocumentType.TAX,
                "Tax Document"
        );

        names.put(
                AiDocumentType.LEGAL,
                "Legal Document"
        );

        names.put(
                AiDocumentType.EMPLOYMENT,
                "Employment Document"
        );

        names.put(
                AiDocumentType.VEHICLE,
                "Vehicle Document"
        );

        names.put(
                AiDocumentType.INVESTMENT,
                "Investment Document"
        );

        names.put(
                AiDocumentType.UTILITY,
                "Utility Document"
        );

        names.put(
                AiDocumentType.OTHER,
                "Other Document"
        );

        return Collections.unmodifiableMap(
                names
        );
    }

    private static Map<AiDocumentType, List<String>>
    createRequiredFields() {

        Map<AiDocumentType, List<String>> fields =
                new EnumMap<>(
                        AiDocumentType.class
                );

        fields.put(
                AiDocumentType.IDENTITY,
                List.of(
                        "name",
                        "dateOfBirth"
                )
        );

        fields.put(
                AiDocumentType.BANKING,
                List.of(
                        "accountNumber",
                        "ifscCode"
                )
        );

        fields.put(
                AiDocumentType.INSURANCE,
                List.of(
                        "policyNumber",
                        "policyHolder",
                        "expiryDate"
                )
        );

        fields.put(
                AiDocumentType.PROPERTY,
                List.of(
                        "ownerName",
                        "registrationNumber",
                        "propertyAddress"
                )
        );

        fields.put(
                AiDocumentType.MEDICAL,
                List.of(
                        "patientName",
                        "reportDate"
                )
        );

        fields.put(
                AiDocumentType.EDUCATION,
                List.of(
                        "studentName",
                        "institutionName",
                        "courseName"
                )
        );

        fields.put(
                AiDocumentType.TAX,
                List.of(
                        "taxpayerName",
                        "panNumber",
                        "assessmentYear"
                )
        );

        fields.put(
                AiDocumentType.LEGAL,
                List.of(
                        "documentNumber",
                        "executionDate"
                )
        );

        fields.put(
                AiDocumentType.EMPLOYMENT,
                List.of(
                        "employeeName",
                        "companyName",
                        "designation"
                )
        );

        fields.put(
                AiDocumentType.VEHICLE,
                List.of(
                        "ownerName",
                        "registrationNumber"
                )
        );

        fields.put(
                AiDocumentType.INVESTMENT,
                List.of(
                        "investorName",
                        "fundName"
                )
        );

        fields.put(
                AiDocumentType.UTILITY,
                List.of(
                        "consumerNumber",
                        "billAmount",
                        "dueDate"
                )
        );

        fields.put(
                AiDocumentType.OTHER,
                Collections.emptyList()
        );

        return Collections.unmodifiableMap(
                fields
        );
    }
}