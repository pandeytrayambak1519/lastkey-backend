package com.lastkey.backend.notification.service.impl;

import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.entity.NomineeDocumentAccess;
import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.notification.service.NotificationEventService;
import com.lastkey.backend.notification.service.NotificationService;
import com.lastkey.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventServiceImpl
        implements NotificationEventService {

    private final NotificationService notificationService;

    /*
     * =========================================================
     * DOCUMENT EVENTS
     * =========================================================
     */

    @Override
    public void documentUploaded(Document document) {

        User owner = requireDocumentOwner(document);

        notificationService.createNotification(
                owner,
                "Document Uploaded",
                "Your document \"" + getDocumentTitle(document)
                        + "\" has been uploaded successfully.",
                NotificationType.DOCUMENT_UPLOADED,
                NotificationPriority.MEDIUM,
                "/documents/" + document.getId(),
                "DOCUMENT",
                document.getId().toString(),
                null
        );
    }

    @Override
    public void documentUpdated(Document document) {

        User owner = requireDocumentOwner(document);

        notificationService.createNotification(
                owner,
                "Document Updated",
                "Your document \"" + getDocumentTitle(document)
                        + "\" has been updated.",
                NotificationType.DOCUMENT_UPDATED,
                NotificationPriority.MEDIUM,
                "/documents/" + document.getId(),
                "DOCUMENT",
                document.getId().toString(),
                null
        );
    }

    @Override
    public void documentDeleted(Document document) {

        User owner = requireDocumentOwner(document);

        notificationService.createNotification(
                owner,
                "Document Deleted",
                "Your document \"" + getDocumentTitle(document)
                        + "\" has been deleted.",
                NotificationType.DOCUMENT_DELETED,
                NotificationPriority.HIGH,
                null,
                "DOCUMENT",
                document.getId().toString(),
                null
        );
    }

    @Override
    public void documentExpiryReminder(
            Document document,
            int daysRemaining
    ) {

        User owner = requireDocumentOwner(document);

        if (daysRemaining != 30
                && daysRemaining != 7
                && daysRemaining != 1) {

            throw new IllegalArgumentException(
                    "Supported reminder days are 30, 7 and 1"
            );
        }

        String dayText =
                daysRemaining == 1
                        ? "day"
                        : "days";

        notificationService.createNotification(
                owner,
                "Document Expiry Reminder",
                "Your document \""
                        + getDocumentTitle(document)
                        + "\" will expire in "
                        + daysRemaining
                        + " "
                        + dayText
                        + ".",
                NotificationType.DOCUMENT_EXPIRY_REMINDER,
                daysRemaining == 1
                        ? NotificationPriority.HIGH
                        : NotificationPriority.MEDIUM,
                "/documents/" + document.getId(),
                "DOCUMENT",
                document.getId().toString(),
                "DOCUMENT_EXPIRY_"
                        + daysRemaining
                        + "_"
                        + document.getId()
        );
    }

    @Override
    public void documentExpired(Document document) {

        User owner = requireDocumentOwner(document);

        notificationService.createNotification(
                owner,
                "Document Expired",
                "Your document \""
                        + getDocumentTitle(document)
                        + "\" expired on "
                        + document.getExpiryDate()
                        + ".",
                NotificationType.DOCUMENT_EXPIRED,
                NotificationPriority.HIGH,
                "/documents/" + document.getId(),
                "DOCUMENT",
                document.getId().toString(),
                "DOCUMENT_EXPIRED_"
                        + document.getId()
        );
    }

    /*
     * =========================================================
     * CATEGORY EVENTS
     * =========================================================
     */

    @Override
    public void categoryCreated(Category category) {

        User owner = requireCategoryOwner(category);

        notificationService.createNotification(
                owner,
                "Category Created",
                "Category \"" + getCategoryName(category)
                        + "\" has been created successfully.",
                NotificationType.CATEGORY_CREATED,
                NotificationPriority.LOW,
                "/categories/" + category.getId(),
                "CATEGORY",
                category.getId().toString(),
                null
        );
    }

    @Override
    public void categoryUpdated(Category category) {

        User owner = requireCategoryOwner(category);

        notificationService.createNotification(
                owner,
                "Category Updated",
                "Category \"" + getCategoryName(category)
                        + "\" has been updated.",
                NotificationType.CATEGORY_UPDATED,
                NotificationPriority.LOW,
                "/categories/" + category.getId(),
                "CATEGORY",
                category.getId().toString(),
                null
        );
    }

    @Override
    public void categoryDeleted(Category category) {

        User owner = requireCategoryOwner(category);

        notificationService.createNotification(
                owner,
                "Category Deleted",
                "Category \"" + getCategoryName(category)
                        + "\" has been deleted.",
                NotificationType.CATEGORY_DELETED,
                NotificationPriority.MEDIUM,
                null,
                "CATEGORY",
                category.getId().toString(),
                null
        );
    }

    /*
     * =========================================================
     * NOMINEE EVENTS
     * =========================================================
     */

    @Override
    public void nomineeAdded(Nominee nominee) {

        User owner = requireNomineeOwner(nominee);

        notificationService.createNotification(
                owner,
                "Nominee Added",
                getNomineeName(nominee)
                        + " has been added as your nominee.",
                NotificationType.NOMINEE_ADDED,
                NotificationPriority.MEDIUM,
                "/nominees",
                "NOMINEE",
                nominee.getId().toString(),
                null
        );
    }

    @Override
    public void nomineeUpdated(Nominee nominee) {

        User owner = requireNomineeOwner(nominee);

        notificationService.createNotification(
                owner,
                "Nominee Updated",
                getNomineeName(nominee)
                        + " has been updated successfully.",
                NotificationType.NOMINEE_UPDATED,
                NotificationPriority.MEDIUM,
                "/nominees",
                "NOMINEE",
                nominee.getId().toString(),
                null
        );
    }

    @Override
    public void nomineeRemoved(Nominee nominee) {

        User owner = requireNomineeOwner(nominee);

        notificationService.createNotification(
                owner,
                "Nominee Removed",
                getNomineeName(nominee)
                        + " has been removed from your nominees.",
                NotificationType.NOMINEE_REMOVED,
                NotificationPriority.HIGH,
                "/nominees",
                "NOMINEE",
                nominee.getId().toString(),
                null
        );
    }

    @Override
    public void nomineeDocumentAssigned(
            NomineeDocumentAccess access
    ) {

        User owner =
                requireNomineeDocumentAccessOwner(access);

        notificationService.createNotification(
                owner,
                "Document Assigned to Nominee",
                "Document \""
                        + getDocumentTitle(access.getDocument())
                        + "\" has been assigned to "
                        + getNomineeName(access.getNominee())
                        + ".",
                NotificationType.NOMINEE_DOCUMENT_ASSIGNED,
                NotificationPriority.MEDIUM,
                "/nominees/" + access.getNominee().getId(),
                "NOMINEE_DOCUMENT_ACCESS",
                access.getId().toString(),
                null
        );
    }

    @Override
    public void nomineeDocumentAccessUpdated(
            NomineeDocumentAccess access
    ) {

        User owner =
                requireNomineeDocumentAccessOwner(access);

        notificationService.createNotification(
                owner,
                "Nominee Document Access Updated",
                "Access permissions for document \""
                        + getDocumentTitle(access.getDocument())
                        + "\" assigned to "
                        + getNomineeName(access.getNominee())
                        + " have been updated.",
                NotificationType.NOMINEE_DOCUMENT_ACCESS_UPDATED,
                NotificationPriority.MEDIUM,
                "/nominees/" + access.getNominee().getId(),
                "NOMINEE_DOCUMENT_ACCESS",
                access.getId().toString(),
                null
        );
    }

    @Override
    public void nomineeDocumentAccessRemoved(
            Nominee nominee,
            Document document
    ) {

        User owner = requireNomineeOwner(nominee);

        if (document == null
                || document.getId() == null) {

            throw new IllegalArgumentException(
                    "Document and document ID are required for notification"
            );
        }

        notificationService.createNotification(
                owner,
                "Nominee Document Access Removed",
                "Access to document \""
                        + getDocumentTitle(document)
                        + "\" has been removed from "
                        + getNomineeName(nominee)
                        + ".",
                NotificationType.NOMINEE_DOCUMENT_ACCESS_REMOVED,
                NotificationPriority.HIGH,
                "/nominees/" + nominee.getId(),
                "DOCUMENT",
                document.getId().toString(),
                null
        );
    }

    /*
     * =========================================================
     * EMERGENCY EVENTS
     * =========================================================
     */

    @Override
    public void emergencyCreated(
            EmergencyRequest request
    ) {

        User owner = requireEmergencyOwner(request);

        notificationService.createNotification(
                owner,
                "Emergency Request Created",
                "An emergency request has been created for nominee "
                        + getNomineeName(request.getNominee())
                        + ".",
                NotificationType.EMERGENCY_REQUEST_CREATED,
                NotificationPriority.CRITICAL,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                null
        );
    }

    @Override
    public void emergencyUpdated(
            EmergencyRequest request
    ) {

        User owner = requireEmergencyOwner(request);

        notificationService.createNotification(
                owner,
                "Emergency Request Updated",
                "Your emergency request for nominee "
                        + getNomineeName(request.getNominee())
                        + " has been updated successfully.",
                NotificationType.EMERGENCY_REQUEST_UPDATED,
                NotificationPriority.HIGH,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                null
        );
    }
    
    @Override
    public void emergencyCancelled(
            EmergencyRequest request
    ) {

        User owner = requireEmergencyOwner(request);

        notificationService.createNotification(
                owner,
                "Emergency Request Cancelled",
                "Your emergency request for nominee "
                        + getNomineeName(request.getNominee())
                        + " has been cancelled.",
                NotificationType.EMERGENCY_CANCELLED,
                NotificationPriority.HIGH,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                null
        );
    }

    @Override
    public void emergencyApproved(
            EmergencyRequest request
    ) {

        User owner = requireEmergencyOwner(request);

        notificationService.createNotification(
                owner,
                "Emergency Request Approved",
                "Your emergency request has been approved.",
                NotificationType.EMERGENCY_APPROVED,
                NotificationPriority.CRITICAL,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                null
        );
    }

    @Override
    public void emergencyRejected(
            EmergencyRequest request
    ) {

        User owner = requireEmergencyOwner(request);

        notificationService.createNotification(
                owner,
                "Emergency Request Rejected",
                "Your emergency request has been rejected.",
                NotificationType.EMERGENCY_REJECTED,
                NotificationPriority.HIGH,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                null
        );
    }

    @Override
    public void documentsReleased(
            EmergencyRequest request,
            int releasedDocumentCount
    ) {

        User owner = requireEmergencyOwner(request);

        if (releasedDocumentCount <= 0) {

            throw new IllegalArgumentException(
                    "Released document count must be greater than zero"
            );
        }

        String documentText =
                releasedDocumentCount == 1
                        ? "document has"
                        : "documents have";

        notificationService.createNotification(
                owner,
                "Documents Released",
                releasedDocumentCount
                        + " "
                        + documentText
                        + " been released to nominee "
                        + getNomineeName(request.getNominee())
                        + ".",
                NotificationType.DOCUMENT_RELEASED,
                NotificationPriority.CRITICAL,
                "/emergencies/" + request.getId(),
                "EMERGENCY",
                request.getId().toString(),
                "EMERGENCY_DOCUMENTS_RELEASED_"
                        + request.getId()
        );
    }

    @Override
    public void accessExpired(
            EmergencyReleaseHistory releaseHistory
    ) {

        if (releaseHistory == null) {

            throw new IllegalArgumentException(
                    "Emergency release history is required for notification"
            );
        }

        if (releaseHistory.getId() == null) {

            throw new IllegalArgumentException(
                    "Emergency release history ID is required for notification"
            );
        }

        EmergencyRequest request =
                releaseHistory.getEmergencyRequest();

        if (request == null) {

            throw new IllegalArgumentException(
                    "Emergency request is required for access-expiry notification"
            );
        }

        User owner = requireEmergencyOwner(request);

        String documentTitle =
                releaseHistory.getDocument() != null
                        ? getDocumentTitle(
                                releaseHistory.getDocument()
                        )
                        : "Unknown Document";

        notificationService.createNotification(
                owner,
                "Document Access Expired",
                "Temporary access to document \""
                        + documentTitle
                        + "\" for nominee "
                        + getNomineeName(request.getNominee())
                        + " has expired.",
                NotificationType.DOCUMENT_ACCESS_REVOKED,
                NotificationPriority.HIGH,
                "/emergencies/" + request.getId(),
                "EMERGENCY_RELEASE_HISTORY",
                releaseHistory.getId().toString(),
                "EMERGENCY_ACCESS_EXPIRED_"
                        + releaseHistory.getId()
        );
    }

    /*
     * =========================================================
     * VALIDATION HELPERS
     * =========================================================
     */

    private User requireDocumentOwner(
            Document document
    ) {

        if (document == null) {

            throw new IllegalArgumentException(
                    "Document is required for notification"
            );
        }

        if (document.getId() == null) {

            throw new IllegalArgumentException(
                    "Document ID is required for notification"
            );
        }

        if (document.getOwner() == null) {

            throw new IllegalArgumentException(
                    "Document owner is required for notification"
            );
        }

        return document.getOwner();
    }

    private User requireCategoryOwner(
            Category category
    ) {

        if (category == null) {

            throw new IllegalArgumentException(
                    "Category is required for notification"
            );
        }

        if (category.getId() == null) {

            throw new IllegalArgumentException(
                    "Category ID is required for notification"
            );
        }

        if (category.getOwner() == null) {

            throw new IllegalArgumentException(
                    "Category owner is required for notification"
            );
        }

        return category.getOwner();
    }

    private User requireNomineeOwner(
            Nominee nominee
    ) {

        if (nominee == null) {

            throw new IllegalArgumentException(
                    "Nominee is required for notification"
            );
        }

        if (nominee.getId() == null) {

            throw new IllegalArgumentException(
                    "Nominee ID is required for notification"
            );
        }

        if (nominee.getOwner() == null) {

            throw new IllegalArgumentException(
                    "Nominee owner is required for notification"
            );
        }

        return nominee.getOwner();
    }

    private User requireNomineeDocumentAccessOwner(
            NomineeDocumentAccess access
    ) {

        if (access == null) {

            throw new IllegalArgumentException(
                    "Nominee document access is required for notification"
            );
        }

        if (access.getId() == null) {

            throw new IllegalArgumentException(
                    "Nominee document access ID is required for notification"
            );
        }

        if (access.getNominee() == null) {

            throw new IllegalArgumentException(
                    "Nominee is required for document access notification"
            );
        }

        if (access.getDocument() == null) {

            throw new IllegalArgumentException(
                    "Document is required for document access notification"
            );
        }

        return requireNomineeOwner(
                access.getNominee()
        );
    }

    private User requireEmergencyOwner(
            EmergencyRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Emergency request is required for notification"
            );
        }

        if (request.getId() == null) {

            throw new IllegalArgumentException(
                    "Emergency request ID is required for notification"
            );
        }

        if (request.getOwner() == null) {

            throw new IllegalArgumentException(
                    "Emergency request owner is required for notification"
            );
        }

        return request.getOwner();
    }

    /*
     * =========================================================
     * DISPLAY VALUE HELPERS
     * =========================================================
     */

    private String getDocumentTitle(
            Document document
    ) {

        if (document == null) {
            return "Untitled Document";
        }

        String title = document.getTitle();

        if (title == null
                || title.isBlank()) {

            return "Untitled Document";
        }

        return title.trim();
    }

    private String getCategoryName(
            Category category
    ) {

        String name = normalizeNullable(
                category.getName()
        );

        return name != null
                ? name
                : "Unnamed Category";
    }

    private String getNomineeName(
            Nominee nominee
    ) {

        if (nominee == null) {
            return "the selected nominee";
        }

        String firstName = normalizeNullable(
                nominee.getFirstName()
        );

        String lastName = normalizeNullable(
                nominee.getLastName()
        );

        String fullName = String.join(
                " ",
                firstName == null
                        ? ""
                        : firstName,
                lastName == null
                        ? ""
                        : lastName
        ).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        String email = normalizeNullable(
                nominee.getEmail()
        );

        return email != null
                ? email
                : "the selected nominee";
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}