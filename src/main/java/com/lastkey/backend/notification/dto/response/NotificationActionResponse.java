package com.lastkey.backend.notification.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationActionResponse {

    private String message;

    private Integer affectedCount;
}