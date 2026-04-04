package com.drc.aidbridge.modules.shared.enums;

/**
 * Loại chiến lược dispatch volunteer.
 * - BROADCAST: Gửi thông báo đến nhiều volunteer cùng lúc
 * - SEQUENTIAL: Gửi tuần tự từng volunteer một
 */
public enum DispatchType {
    BROADCAST, // Gửi đồng loạt
    SEQUENTIAL // Gửi tuần tự
}
