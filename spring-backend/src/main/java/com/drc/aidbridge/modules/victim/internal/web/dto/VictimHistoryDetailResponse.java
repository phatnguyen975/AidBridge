package com.drc.aidbridge.modules.victim.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Detailed history response used by victim history detail endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimHistoryDetailResponse {

    private String id;
    private String type;
    private String status;
    private String statusType;
    private String createdAt;
    private String location;
    private String condition;
    private Integer peopleCount;

    private Integer numberAdult;
    private Integer numberElderly;
    private Integer numberChildren;

    private String noteFullName;
    private String notePhoneNumber;
    private String noteHealthDetail;

    private List<VictimHistoryAidItemDetailResponse> requestedItems;
}
