package com.lastkey.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalDocuments;

    private long favoriteDocuments;

    private long archivedDocuments;

    private long totalCategories;

    private long totalNominees;

    private long activeEmergencies;

    private long unreadNotifications;

    private long expiringDocuments;
}