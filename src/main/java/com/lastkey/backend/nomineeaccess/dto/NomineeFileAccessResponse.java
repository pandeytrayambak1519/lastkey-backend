package com.lastkey.backend.nomineeaccess.dto;

import lombok.*;
import org.springframework.core.io.Resource;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomineeFileAccessResponse {

    private Resource resource;

    private String fileName;

    private String mimeType;

    private Long fileSize;
}