package com.drc.aidbridge.modules.victim.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response item used by victim history list endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimHistoryItemResponse {

    private String id;
    private String title;
    private String status;
    private String statusType;
    private String createdAt;
    private String location;
    private String type;
    private String note;
}
