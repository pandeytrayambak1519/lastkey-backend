package com.lastkey.backend.nominee.repository;

import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.enums.NomineeStatus;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NomineeRepository
        extends JpaRepository<Nominee, UUID> {

    Optional<Nominee> findByIdAndOwner(
            UUID nomineeId,
            User owner
    );
    
    Optional<Nominee>
    findFirstByEmailIgnoreCaseAndActiveTrue(
            String email
    );

    List<Nominee>
    findByEmailIgnoreCaseAndActiveTrue(
            String email
    );

    boolean existsByEmailIgnoreCaseAndActiveTrue(
            String email
    );

    Optional<Nominee> findByIdAndOwnerAndActiveTrue(
            UUID nomineeId,
            User owner
    );

    Optional<Nominee> findByEmailAndOwner(
            String email,
            User owner
    );

    boolean existsByEmailAndOwner(
            String email,
            User owner
    );

    boolean existsByPhoneAndOwner(
            String phone,
            User owner
    );

    boolean existsByEmailAndOwnerAndIdNot(
            String email,
            User owner,
            UUID nomineeId
    );

    boolean existsByPhoneAndOwnerAndIdNot(
            String phone,
            User owner,
            UUID nomineeId
    );

    Page<Nominee> findByOwnerAndActiveTrue(
            User owner,
            Pageable pageable
    );

    Page<Nominee> findByOwnerAndStatusAndActiveTrue(
            User owner,
            NomineeStatus status,
            Pageable pageable
    );

    List<Nominee> findByOwnerAndPrimaryNomineeTrueAndActiveTrue(
            User owner
    );

    Optional<Nominee> findFirstByOwnerAndPrimaryNomineeTrueAndActiveTrue(
            User owner
    );

    Optional<Nominee> findByVerificationToken(
            String verificationToken
    );

    long countByOwnerAndActiveTrue(
            User owner
    );

    long countByOwnerAndStatusAndActiveTrue(
            User owner,
            NomineeStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Nominee nominee
            set nominee.primaryNominee = false
            where nominee.owner = :owner
              and nominee.id <> :excludedNomineeId
              and nominee.primaryNominee = true
            """)
    int clearPrimaryNomineeFromOthers(
            @Param("owner")
            User owner,

            @Param("excludedNomineeId")
            UUID excludedNomineeId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Nominee nominee
            set nominee.primaryNominee = false
            where nominee.owner = :owner
              and nominee.primaryNominee = true
            """)
    int clearAllPrimaryNominees(
            @Param("owner")
            User owner
    );
}