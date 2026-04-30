package com.drc.aidbridge.modules.sos.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.sos.internal.usecase.CreateSmsIngestSosUseCase;
import com.drc.aidbridge.modules.sos.internal.web.dto.SmsIngestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/sms-ingest")
@RequiredArgsConstructor
public class SmsIngestController {

    private final CreateSmsIngestSosUseCase createSmsIngestSosUseCase;

    @Value("${aidbridge.gateway.sms.token:dev-gateway-token}")
    private String gatewaySmsToken;

    @PostMapping("/sos")
    public ResponseEntity<ApiResponse<SosRequestResponse>> ingestSos(
            @RequestHeader(value = "X-Gateway-Token", required = false) String gatewayToken,
            @Valid @RequestBody SmsIngestSosRequest request) {
        log.info("SMS_INGEST_RECEIVED clientRequestId={}", safeText(request.getClientRequestId()));

        if (!isGatewayTokenValid(gatewayToken)) {
            log.warn("SMS_INGEST_TOKEN_INVALID");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid gateway token"));
        }

        SosRequestResponse response = createSmsIngestSosUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("SMS SOS ingested", response));
    }

    private boolean isGatewayTokenValid(String gatewayToken) {
        return StringUtils.hasText(gatewaySmsToken)
            && StringUtils.hasText(gatewayToken)
            && gatewaySmsToken.trim().equals(gatewayToken.trim());
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
