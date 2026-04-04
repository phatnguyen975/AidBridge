package com.drc.aidbridge.modules.shared.enums;

/**
 * Phản hồi của tình nguyện viên đối với yêu cầu dispatch.
 * Tương ứng với enum dispatch_response trong database schema.
 */
public enum DispatchResponse {
    PENDING, // Đang chờ phản hồi
    ACCEPTED, // Đã chấp nhận
    REJECTED, // Đã từ chối
    TIMEOUT // Hết thời gian chờ
}
