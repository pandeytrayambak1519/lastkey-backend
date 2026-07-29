package com.lastkey.backend.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResponse {

    private List<DocumentSearchItemResponse> documents;

    private int currentPage;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean first;

    private boolean last;

    private boolean hasNext;

    private boolean hasPrevious;
}