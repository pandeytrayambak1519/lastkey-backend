package com.lastkey.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private String userName;

    private String email;

    private DashboardStatsResponse statistics;

    private List<RecentDocumentResponse> recentDocuments;

    private List<RecentNotificationResponse> recentNotifications;

    private LocalDateTime generatedAt;
}