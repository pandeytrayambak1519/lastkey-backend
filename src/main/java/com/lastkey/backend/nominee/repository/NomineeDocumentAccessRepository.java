package com.lastkey.backend.nominee.repository;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.entity.NomineeDocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NomineeDocumentAccessRepository
        extends JpaRepository<NomineeDocumentAccess, UUID> {

    Optional<NomineeDocumentAccess>
    findByNomineeAndDocument(
            Nominee nominee,
            Document document
    );

    boolean existsByNomineeAndDocument(
            Nominee nominee,
            Document document
    );

    List<NomineeDocumentAccess> findByNominee(
            Nominee nominee
    );

    List<NomineeDocumentAccess> findByDocument(
            Document document
    );

    List<NomineeDocumentAccess>
    findByNomineeAndCanViewTrue(
            Nominee nominee
    );

    List<NomineeDocumentAccess>
    findByNomineeAndCanDownloadTrue(
            Nominee nominee
    );

    long countByNominee(
            Nominee nominee
    );

    void deleteByNomineeAndDocument(
            Nominee nominee,
            Document document
    );

    void deleteByNominee(
            Nominee nominee
    );

    void deleteByDocument(
            Document document
    );
}