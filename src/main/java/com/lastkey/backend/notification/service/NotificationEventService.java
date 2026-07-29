package com.lastkey.backend.notification.service;

import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.entity.NomineeDocumentAccess;

public interface NotificationEventService {

    /*
     * =========================================================
     * DOCUMENT EVENTS
     * =========================================================
     */

    void documentUploaded(Document document);

    void documentUpdated(Document document);

    void documentDeleted(Document document);

    void documentExpiryReminder(
            Document document,
            int daysRemaining
    );

    void documentExpired(Document document);


    /*
     * =========================================================
     * CATEGORY EVENTS
     * =========================================================
     */

    void categoryCreated(Category category);

    void categoryUpdated(Category category);

    void categoryDeleted(Category category);


    /*
     * =========================================================
     * NOMINEE EVENTS
     * =========================================================
     */

    void nomineeAdded(Nominee nominee);

    void nomineeUpdated(Nominee nominee);

    void nomineeRemoved(Nominee nominee);

    void nomineeDocumentAssigned(
            NomineeDocumentAccess access
    );

    void nomineeDocumentAccessUpdated(
            NomineeDocumentAccess access
    );

    void nomineeDocumentAccessRemoved(
            Nominee nominee,
            Document document
    );


    /*
     * =========================================================
     * EMERGENCY EVENTS
     * =========================================================
     */

    void emergencyCreated(EmergencyRequest request);

    void emergencyUpdated(EmergencyRequest request);

    void emergencyCancelled(EmergencyRequest request);

    void emergencyApproved(EmergencyRequest request);

    void emergencyRejected(EmergencyRequest request);

    /**
     * Called after documents are successfully released
     * to the nominee.
     */
    void documentsReleased(
            EmergencyRequest request,
            int releasedDocumentCount
    );

    /**
     * Called when nominee access automatically expires
     * and is revoked by the scheduler.
     */
    void accessExpired(
            EmergencyReleaseHistory releaseHistory
    );
}