package com.lastkey.backend.nominee.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NomineeDocumentAccessRequest {

    @NotNull(message = "View permission is required")
    private Boolean canView = true;

    @NotNull(message = "Download permission is required")
    private Boolean canDownload = true;
}