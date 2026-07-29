package com.lastkey.backend.nomineeaccess.dto;

import com.lastkey.backend.nomineeaccess.enums.NomineeAccessAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomineeAccessHistoryResponse {

    private UUID id;

    private UUID documentId;

    private String documentTitle;

    private NomineeAccessAction action;

    private Boolean successful;

    private String failureReason;

    private String ipAddress;

    private LocalDateTime createdAt;
}