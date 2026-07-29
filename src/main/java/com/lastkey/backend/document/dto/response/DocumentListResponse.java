package com.lastkey.backend.document.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentListResponse {

    private List<DocumentResponse> documents;

    private long totalElements;

    private int totalPages;

    private int currentPage;

    private int pageSize;

    private boolean first;

    private boolean last;

    private boolean hasNext;

    private boolean hasPrevious;
}