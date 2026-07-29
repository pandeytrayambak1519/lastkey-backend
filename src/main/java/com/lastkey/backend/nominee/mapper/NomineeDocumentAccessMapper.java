package com.lastkey.backend.nominee.mapper;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.nominee.dto.request.NomineeDocumentAccessRequest;
import com.lastkey.backend.nominee.dto.request.NomineeDocumentAccessRequest;
import com.lastkey.backend.nominee.dto.response.NomineeDocumentResponse;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.entity.NomineeDocumentAccess;
import org.springframework.stereotype.Component;

@Component
public class NomineeDocumentAccessMapper {

    public NomineeDocumentAccess toEntity(
            Nominee nominee,
            Document document,
            NomineeDocumentAccessRequest request
    ) {
        return NomineeDocumentAccess.builder()
                .nominee(nominee)
                .document(document)
                .canView(request.getCanView())
                .canDownload(request.getCanDownload())
                .build();
    }

    public NomineeDocumentResponse toResponse(
            NomineeDocumentAccess access
    ) {
        Document document = access.getDocument();

        return NomineeDocumentResponse.builder()
                .accessId(access.getId())
                .nomineeId(access.getNominee().getId())
                .documentId(document.getId())
                .documentName(resolveDocumentName(document))
                .fileType(resolveFileType(document))
                .canView(access.getCanView())
                .canDownload(access.getCanDownload())
                .grantedAt(access.getGrantedAt())
                .updatedAt(access.getUpdatedAt())
                .build();
    }

    public void updatePermissions(
            NomineeDocumentAccess access,
            NomineeDocumentAccessRequest request
    ) {
        access.setCanView(request.getCanView());
        access.setCanDownload(request.getCanDownload());
    }

    private String resolveDocumentName(Document document) {
        /*
         * Replace getTitle() with getName() or getOriginalFileName()
         * if your Document entity uses a different field.
         */
        return document.getTitle();
    }

    private String resolveFileType(Document document) {
        /*
         * Prefer MIME type for the frontend, for example:
         * application/pdf, image/jpeg.
         */
        return document.getMimeType();
    }
}