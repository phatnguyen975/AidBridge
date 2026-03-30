package com.drc.aidbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelMissionRequestDto {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
