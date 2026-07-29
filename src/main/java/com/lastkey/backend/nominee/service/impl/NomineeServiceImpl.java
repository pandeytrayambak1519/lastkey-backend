package com.lastkey.backend.nominee.service.impl;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.email.enums.OtpPurpose;
import com.lastkey.backend.email.service.OtpService;
import com.lastkey.backend.nominee.dto.request.NomineeCreateRequest;
import com.lastkey.backend.nominee.dto.request.NomineeDocumentAccessRequest;
import com.lastkey.backend.nominee.dto.request.NomineeUpdateRequest;
import com.lastkey.backend.nominee.dto.response.NomineeDocumentResponse;
import com.lastkey.backend.nominee.dto.response.NomineeResponse;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.entity.NomineeDocumentAccess;
import com.lastkey.backend.nominee.enums.NomineeStatus;
import com.lastkey.backend.nominee.enums.RelationshipType;
import com.lastkey.backend.nominee.exception.NomineeAlreadyExistsException;
import com.lastkey.backend.nominee.exception.NomineeDocumentAccessNotFoundException;
import com.lastkey.backend.nominee.exception.NomineeDocumentAlreadyAssignedException;
import com.lastkey.backend.nominee.exception.NomineeNotFoundException;
import com.lastkey.backend.nominee.mapper.NomineeDocumentAccessMapper;
import com.lastkey.backend.nominee.mapper.NomineeMapper;
import com.lastkey.backend.nominee.repository.NomineeDocumentAccessRepository;
import com.lastkey.backend.nominee.repository.NomineeRepository;
import com.lastkey.backend.nominee.service.NomineeService;
import com.lastkey.backend.notification.service.NotificationEventService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NomineeServiceImpl implements NomineeService {

    private final NomineeRepository nomineeRepository;

    private final NomineeDocumentAccessRepository
            nomineeDocumentAccessRepository;

    private final UserRepository userRepository;

    private final DocumentRepository documentRepository;

    private final NomineeMapper nomineeMapper;

    private final NomineeDocumentAccessMapper
            nomineeDocumentAccessMapper;

    private final NotificationEventService
            notificationEventService;

    /*
     * Central OTP service is used for nominee verification.
     */
    private final OtpService otpService;

    /*
     * =========================================================
     * CREATE NOMINEE
     * =========================================================
     */

    @Override
    public NomineeResponse createNominee(
            NomineeCreateRequest request,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        String normalizedPhone =
                normalizePhone(request.getPhone());

        validateRelationship(
                request.getRelationship(),
                request.getCustomRelationship()
        );

        validateDuplicateEmailForCreation(
                owner,
                normalizedEmail
        );

        validateDuplicatePhoneForCreation(
                owner,
                normalizedPhone
        );

        request.setEmail(normalizedEmail);
        request.setPhone(normalizedPhone);

        if (request.getRelationship()
                != RelationshipType.OTHER) {

            request.setCustomRelationship(null);
        }

        Nominee nominee =
                nomineeMapper.toEntity(request);

        nominee.setOwner(owner);

        nominee.setStatus(
                NomineeStatus.PENDING_VERIFICATION
        );

        nominee.setEmailVerified(false);
        nominee.setPhoneVerified(false);
        nominee.setActive(true);

        nominee.setVerifiedAt(null);
        nominee.setVerificationToken(null);
        nominee.setVerificationTokenExpiry(null);

        Nominee savedNominee =
                nomineeRepository.save(nominee);

        if (Boolean.TRUE.equals(
                savedNominee.getPrimaryNominee()
        )) {

            nomineeRepository
                    .clearPrimaryNomineeFromOthers(
                            owner,
                            savedNominee.getId()
                    );
        }

        /*
         * Generate and send a verification OTP immediately
         * after the nominee is created.
         */
        sendNomineeVerificationOtp(savedNominee);

        notificationEventService.nomineeAdded(
                savedNominee
        );

        return buildNomineeResponse(savedNominee);
    }

    /*
     * =========================================================
     * GET NOMINEE BY ID
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public NomineeResponse getNomineeById(
            UUID nomineeId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        return buildNomineeResponse(nominee);
    }

    /*
     * =========================================================
     * GET ALL NOMINEES
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public Page<NomineeResponse> getAllNominees(
            String userEmail,
            NomineeStatus status,
            Pageable pageable
    ) {

        User owner = getCurrentUser(userEmail);

        Page<Nominee> nominees;

        if (status == null) {

            nominees =
                    nomineeRepository
                            .findByOwnerAndActiveTrue(
                                    owner,
                                    pageable
                            );

        } else {

            nominees =
                    nomineeRepository
                            .findByOwnerAndStatusAndActiveTrue(
                                    owner,
                                    status,
                                    pageable
                            );
        }

        return nominees.map(
                this::buildNomineeResponse
        );
    }

    /*
     * =========================================================
     * UPDATE NOMINEE
     * =========================================================
     */

    @Override
    public NomineeResponse updateNominee(
            UUID nomineeId,
            NomineeUpdateRequest request,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        /*
         * Store the old email so that we can detect whether
         * the nominee must complete verification again.
         */
        String previousEmail =
                nominee.getEmail();

        updateBasicDetails(
                nominee,
                request,
                owner
        );

        updateRelationship(
                nominee,
                request
        );

        updatePrimaryStatus(
                nominee,
                request,
                owner
        );

        if (request.getActive() != null) {

            nominee.setActive(
                    request.getActive()
            );

            if (!Boolean.TRUE.equals(
                    request.getActive()
            )) {

                nominee.setPrimaryNominee(false);

                nominee.setStatus(
                        NomineeStatus.INACTIVE
                );

            } else if (nominee.getStatus()
                    == NomineeStatus.INACTIVE) {

                nominee.setStatus(
                        Boolean.TRUE.equals(
                                nominee.getEmailVerified()
                        )
                                ? NomineeStatus.VERIFIED
                                : NomineeStatus.PENDING_VERIFICATION
                );
            }
        }

        if (request.getNotes() != null) {

            nominee.setNotes(
                    normalizeNullable(
                            request.getNotes()
                    )
            );
        }

        Nominee updatedNominee =
                nomineeRepository.save(nominee);

        boolean emailChanged =
                previousEmail != null
                        && updatedNominee.getEmail() != null
                        && !previousEmail.equalsIgnoreCase(
                                updatedNominee.getEmail()
                        );

        /*
         * Invalidate OTPs linked to the previous email and
         * send a new OTP to the updated nominee email.
         */
        if (emailChanged
                && Boolean.TRUE.equals(
                updatedNominee.getActive()
        )) {

            otpService.invalidateOtps(
                    previousEmail,
                    OtpPurpose.NOMINEE_VERIFICATION
            );

            otpService.invalidateOtps(
                    updatedNominee.getEmail(),
                    OtpPurpose.NOMINEE_VERIFICATION
            );

            sendNomineeVerificationOtp(
                    updatedNominee
            );
        }

        notificationEventService.nomineeUpdated(
                updatedNominee
        );

        return buildNomineeResponse(
                updatedNominee
        );
    }

    /*
     * =========================================================
     * DELETE NOMINEE
     * =========================================================
     */

    @Override
    public void deleteNominee(
            UUID nomineeId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        /*
         * Soft delete is safer because nominee history may be
         * required by emergency and audit modules.
         */
        nominee.setActive(false);
        nominee.setPrimaryNominee(false);

        nominee.setStatus(
                NomineeStatus.INACTIVE
        );

        otpService.invalidateOtps(
                nominee.getEmail(),
                OtpPurpose.NOMINEE_VERIFICATION
        );

        Nominee deletedNominee =
                nomineeRepository.save(nominee);

        notificationEventService.nomineeRemoved(
                deletedNominee
        );
    }

    /*
     * =========================================================
     * SET PRIMARY NOMINEE
     * =========================================================
     */

    @Override
    public NomineeResponse setPrimaryNominee(
            UUID nomineeId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        if (!Boolean.TRUE.equals(
                nominee.getActive()
        )) {

            throw new IllegalStateException(
                    "Inactive nominee cannot be made primary"
            );
        }

        nomineeRepository
                .clearPrimaryNomineeFromOthers(
                        owner,
                        nominee.getId()
                );

        nominee.setPrimaryNominee(true);

        Nominee savedNominee =
                nomineeRepository.save(nominee);

        return buildNomineeResponse(
                savedNominee
        );
    }

    /*
     * =========================================================
     * RESEND NOMINEE VERIFICATION OTP
     * =========================================================
     */

    @Override
    public NomineeResponse resendVerificationOtp(
            UUID nomineeId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        validateNomineeCanBeVerified(nominee);

        /*
         * Invalidate the previous OTP before creating another
         * one so that only the latest OTP remains usable.
         */
        otpService.invalidateOtps(
                nominee.getEmail(),
                OtpPurpose.NOMINEE_VERIFICATION
        );

        sendNomineeVerificationOtp(nominee);

        return buildNomineeResponse(nominee);
    }

    /*
     * =========================================================
     * VERIFY NOMINEE EMAIL
     * =========================================================
     */

    @Override
    public NomineeResponse verifyNominee(
            UUID nomineeId,
            String otp,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        /*
         * Return the existing state when the nominee is already
         * verified. This makes the operation idempotent.
         */
        if (Boolean.TRUE.equals(
                nominee.getEmailVerified()
        ) && nominee.getStatus()
                == NomineeStatus.VERIFIED) {

            return buildNomineeResponse(nominee);
        }

        if (!Boolean.TRUE.equals(
                nominee.getActive()
        )) {

            throw new IllegalStateException(
                    "Inactive nominee cannot be verified"
            );
        }

        String normalizedOtp =
                normalizeOtp(otp);

        boolean otpVerified =
                otpService.verifyOtp(
                        nominee.getEmail(),
                        normalizedOtp,
                        OtpPurpose.NOMINEE_VERIFICATION
                );

        if (!otpVerified) {

            throw new IllegalArgumentException(
                    "Invalid or expired nominee verification OTP"
            );
        }

        nominee.setEmailVerified(true);

        nominee.setStatus(
                NomineeStatus.VERIFIED
        );

        nominee.setVerifiedAt(
                LocalDateTime.now()
        );

        /*
         * The central OTP module now manages verification data,
         * so the old nominee token fields are cleared.
         */
        nominee.setVerificationToken(null);
        nominee.setVerificationTokenExpiry(null);

        Nominee verifiedNominee =
                nomineeRepository.save(nominee);

        return buildNomineeResponse(
                verifiedNominee
        );
    }

    /*
     * =========================================================
     * PART 2 CONTINUES FROM HERE:
     * ASSIGN DOCUMENT TO NOMINEE
     * =========================================================
     */
    
    @Override
    public NomineeDocumentResponse assignDocument(
            UUID nomineeId,
            UUID documentId,
            NomineeDocumentAccessRequest request,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        Document document =
                getOwnedDocument(
                        documentId,
                        owner
                );

        boolean alreadyAssigned =
                nomineeDocumentAccessRepository
                        .existsByNomineeAndDocument(
                                nominee,
                                document
                        );

        if (alreadyAssigned) {

            throw new NomineeDocumentAlreadyAssignedException(
                    "Document is already assigned to this nominee"
            );
        }

        validateDocumentPermissions(request);

        NomineeDocumentAccess access =
                nomineeDocumentAccessMapper.toEntity(
                        nominee,
                        document,
                        request
                );

        NomineeDocumentAccess savedAccess =
                nomineeDocumentAccessRepository
                        .save(access);

        notificationEventService
                .nomineeDocumentAssigned(savedAccess);

        return nomineeDocumentAccessMapper
                .toResponse(savedAccess);
    }

    /*
     * =========================================================
     * UPDATE DOCUMENT ACCESS
     * =========================================================
     */

    @Override
    public NomineeDocumentResponse updateDocumentAccess(
            UUID nomineeId,
            UUID documentId,
            NomineeDocumentAccessRequest request,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        Document document =
                getOwnedDocument(
                        documentId,
                        owner
                );

        NomineeDocumentAccess access =
                nomineeDocumentAccessRepository
                        .findByNomineeAndDocument(
                                nominee,
                                document
                        )
                        .orElseThrow(() ->
                                new NomineeDocumentAccessNotFoundException(
                                        "Document access was not found for this nominee"
                                )
                        );

        validateDocumentPermissions(request);

        nomineeDocumentAccessMapper
                .updatePermissions(
                        access,
                        request
                );

        NomineeDocumentAccess updatedAccess =
                nomineeDocumentAccessRepository
                        .save(access);

        notificationEventService
                .nomineeDocumentAccessUpdated(
                        updatedAccess
                );

        return nomineeDocumentAccessMapper
                .toResponse(updatedAccess);
    }

    /*
     * =========================================================
     * REMOVE DOCUMENT ACCESS
     * =========================================================
     */

    @Override
    public void removeDocumentAccess(
            UUID nomineeId,
            UUID documentId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        Document document =
                getOwnedDocument(
                        documentId,
                        owner
                );

        NomineeDocumentAccess access =
                nomineeDocumentAccessRepository
                        .findByNomineeAndDocument(
                                nominee,
                                document
                        )
                        .orElseThrow(() ->
                                new NomineeDocumentAccessNotFoundException(
                                        "Document access was not found for this nominee"
                                )
                        );

        nomineeDocumentAccessRepository.delete(access);

        notificationEventService
                .nomineeDocumentAccessRemoved(
                        nominee,
                        document
                );
    }

    /*
     * =========================================================
     * GET ASSIGNED DOCUMENTS
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public List<NomineeDocumentResponse> getAssignedDocuments(
            UUID nomineeId,
            String userEmail
    ) {

        User owner = getCurrentUser(userEmail);

        Nominee nominee =
                getActiveNominee(
                        nomineeId,
                        owner
                );

        return nomineeDocumentAccessRepository
                .findByNominee(nominee)
                .stream()
                .map(
                        nomineeDocumentAccessMapper
                                ::toResponse
                )
                .toList();
    }

    /*
     * =========================================================
     * UPDATE HELPER METHODS
     * =========================================================
     */

    private void updateBasicDetails(
            Nominee nominee,
            NomineeUpdateRequest request,
            User owner
    ) {

        if (request.getFirstName() != null) {

            nominee.setFirstName(
                    request.getFirstName().trim()
            );
        }

        if (request.getLastName() != null) {

            nominee.setLastName(
                    request.getLastName().trim()
            );
        }

        if (request.getEmail() != null) {

            String normalizedEmail =
                    normalizeEmail(
                            request.getEmail()
                    );

            boolean emailChanged =
                    !normalizedEmail.equalsIgnoreCase(
                            nominee.getEmail()
                    );

            if (emailChanged) {

                validateDuplicateEmailForUpdate(
                        owner,
                        normalizedEmail,
                        nominee.getId()
                );

                nominee.setEmail(normalizedEmail);

                nominee.setEmailVerified(false);
                nominee.setVerifiedAt(null);
                nominee.setVerificationToken(null);
                nominee.setVerificationTokenExpiry(null);

                nominee.setStatus(
                        NomineeStatus.PENDING_VERIFICATION
                );
            }
        }

        if (request.getPhone() != null) {

            String normalizedPhone =
                    normalizePhone(
                            request.getPhone()
                    );

            boolean phoneChanged =
                    !normalizedPhone.equals(
                            nominee.getPhone()
                    );

            if (phoneChanged) {

                validateDuplicatePhoneForUpdate(
                        owner,
                        normalizedPhone,
                        nominee.getId()
                );

                nominee.setPhone(normalizedPhone);
                nominee.setPhoneVerified(false);
            }
        }
    }

    private void updateRelationship(
            Nominee nominee,
            NomineeUpdateRequest request
    ) {

        if (request.getRelationship() != null) {

            RelationshipType relationship =
                    request.getRelationship();

            String customRelationship =
                    request.getCustomRelationship();

            validateRelationship(
                    relationship,
                    customRelationship
            );

            nominee.setRelationship(relationship);

            if (relationship
                    == RelationshipType.OTHER) {

                nominee.setCustomRelationship(
                        normalizeNullable(
                                customRelationship
                        )
                );

            } else {

                nominee.setCustomRelationship(null);
            }

            return;
        }

        if (request.getCustomRelationship() != null) {

            if (nominee.getRelationship()
                    != RelationshipType.OTHER) {

                throw new IllegalArgumentException(
                        "Custom relationship can only be used when relationship is OTHER"
                );
            }

            validateRelationship(
                    RelationshipType.OTHER,
                    request.getCustomRelationship()
            );

            nominee.setCustomRelationship(
                    normalizeNullable(
                            request.getCustomRelationship()
                    )
            );
        }
    }

    private void updatePrimaryStatus(
            Nominee nominee,
            NomineeUpdateRequest request,
            User owner
    ) {

        if (request.getPrimaryNominee() == null) {
            return;
        }

        if (Boolean.TRUE.equals(
                request.getPrimaryNominee()
        )) {

            nomineeRepository
                    .clearPrimaryNomineeFromOthers(
                            owner,
                            nominee.getId()
                    );

            nominee.setPrimaryNominee(true);

        } else {

            nominee.setPrimaryNominee(false);
        }
    }

    /*
     * =========================================================
     * PART 2B CONTINUES FROM HERE:
     * VALIDATION METHODS
     * =========================================================
     */
    
    /*
     * =========================================================
     * VALIDATION METHODS
     * =========================================================
     */

    private void validateRelationship(
            RelationshipType relationship,
            String customRelationship
    ) {

        if (relationship == null) {

            throw new IllegalArgumentException(
                    "Relationship is required"
            );
        }

        if (relationship == RelationshipType.OTHER
                && (customRelationship == null
                || customRelationship.isBlank())) {

            throw new IllegalArgumentException(
                    "Custom relationship is required when relationship is OTHER"
            );
        }
    }

    private void validateDuplicateEmailForCreation(
            User owner,
            String email
    ) {

        if (nomineeRepository.existsByEmailAndOwner(
                email,
                owner
        )) {

            throw new NomineeAlreadyExistsException(
                    "A nominee with this email already exists"
            );
        }
    }

    private void validateDuplicatePhoneForCreation(
            User owner,
            String phone
    ) {

        if (nomineeRepository.existsByPhoneAndOwner(
                phone,
                owner
        )) {

            throw new NomineeAlreadyExistsException(
                    "A nominee with this phone number already exists"
            );
        }
    }

    private void validateDuplicateEmailForUpdate(
            User owner,
            String email,
            UUID nomineeId
    ) {

        if (nomineeRepository
                .existsByEmailAndOwnerAndIdNot(
                        email,
                        owner,
                        nomineeId
                )) {

            throw new NomineeAlreadyExistsException(
                    "Another nominee already uses this email"
            );
        }
    }

    private void validateDuplicatePhoneForUpdate(
            User owner,
            String phone,
            UUID nomineeId
    ) {

        if (nomineeRepository
                .existsByPhoneAndOwnerAndIdNot(
                        phone,
                        owner,
                        nomineeId
                )) {

            throw new NomineeAlreadyExistsException(
                    "Another nominee already uses this phone number"
            );
        }
    }

    private void validateDocumentPermissions(
            NomineeDocumentAccessRequest request
    ) {

        if (request.getCanView() == null) {

            throw new IllegalArgumentException(
                    "View permission is required"
            );
        }

        if (request.getCanDownload() == null) {

            throw new IllegalArgumentException(
                    "Download permission is required"
            );
        }

        if (Boolean.TRUE.equals(
                request.getCanDownload()
        ) && !Boolean.TRUE.equals(
                request.getCanView()
        )) {

            throw new IllegalArgumentException(
                    "Download permission requires view permission"
            );
        }
    }

    /*
     * =========================================================
     * OTP VALIDATION METHODS
     * =========================================================
     */

    private void validateNomineeCanBeVerified(
            Nominee nominee
    ) {

        if (!Boolean.TRUE.equals(
                nominee.getActive()
        )) {

            throw new IllegalStateException(
                    "Inactive nominee cannot be verified"
            );
        }

        if (Boolean.TRUE.equals(
                nominee.getEmailVerified()
        )) {

            throw new IllegalStateException(
                    "Nominee email is already verified"
            );
        }
    }

    private String normalizeOtp(
            String otp
    ) {

        if (otp == null) {

            throw new IllegalArgumentException(
                    "OTP is required"
            );
        }

        String normalizedOtp =
                otp.trim();

        if (!normalizedOtp.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }

        return normalizedOtp;
    }

    /*
     * =========================================================
     * ENTITY FETCHING METHODS
     * =========================================================
     */

    private User getCurrentUser(
            String email
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        return userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user was not found"
                        )
                );
    }

    private Nominee getActiveNominee(
            UUID nomineeId,
            User owner
    ) {

        return nomineeRepository
                .findByIdAndOwnerAndActiveTrue(
                        nomineeId,
                        owner
                )
                .orElseThrow(() ->
                        new NomineeNotFoundException(
                                "Nominee not found with id: "
                                        + nomineeId
                        )
                );
    }

    private Document getOwnedDocument(
            UUID documentId,
            User owner
    ) {

        return documentRepository
                .findByIdAndOwner(
                        documentId,
                        owner
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found or you do not have permission to access it"
                        )
                );
    }

    /*
     * =========================================================
     * PART 3 CONTINUES FROM HERE:
     * OTP HELPER METHODS
     * =========================================================
     */
    
    /*
     * =========================================================
     * OTP HELPER METHODS
     * =========================================================
     */

    private void sendNomineeVerificationOtp(
            Nominee nominee
    ) {

        if (nominee == null) {

            throw new IllegalArgumentException(
                    "Nominee is required"
            );
        }

        if (nominee.getEmail() == null
                || nominee.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Nominee email is required"
            );
        }

        otpService.generateAndSendOtp(
                nominee.getEmail(),
                buildNomineeFullName(nominee),
                OtpPurpose.NOMINEE_VERIFICATION
        );
    }

    private String buildNomineeFullName(
            Nominee nominee
    ) {

        String firstName =
                nominee.getFirstName() == null
                        ? ""
                        : nominee.getFirstName().trim();

        String lastName =
                nominee.getLastName() == null
                        ? ""
                        : nominee.getLastName().trim();

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank()
                ? "Nominee"
                : fullName;
    }

    /*
     * =========================================================
     * RESPONSE AND NORMALIZATION METHODS
     * =========================================================
     */

    private NomineeResponse buildNomineeResponse(
            Nominee nominee
    ) {

        long documentCount =
                nomineeDocumentAccessRepository
                        .countByNominee(nominee);

        return nomineeMapper.toResponse(
                nominee,
                documentCount
        );
    }

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        return email
                .trim()
                .toLowerCase();
    }

    private String normalizePhone(
            String phone
    ) {

        if (phone == null
                || phone.isBlank()) {

            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        return phone.trim();
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}