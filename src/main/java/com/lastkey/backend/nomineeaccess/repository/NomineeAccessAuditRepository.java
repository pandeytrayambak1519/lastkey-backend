package com.lastkey.backend.nomineeaccess.repository;

import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nomineeaccess.entity.NomineeAccessAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NomineeAccessAuditRepository
        extends JpaRepository<NomineeAccessAudit, UUID> {

    Page<NomineeAccessAudit>
    findByNomineeOrderByCreatedAtDesc(
            Nominee nominee,
            Pageable pageable
    );
}