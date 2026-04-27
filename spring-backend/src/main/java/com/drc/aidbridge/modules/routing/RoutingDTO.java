package com.drc.aidbridge.modules.routing;

import java.util.List;


/**
 * Public DTO for routing results.
 * Used by other modules (e.g. mission/) via RoutingFacade.
 *
 * @param distance      route distance in meters (m)
 * @param duration      route duration in seconds (s)
 * @param polyline      Google Maps encoded polyline (precision 5)
 * @param timestamp     server timestamp in milliseconds (ms)
 * @param instructions  turn-by-turn directions from start to end
 */
public record RoutingDTO(
        double distance,
        long duration,
        String polyline,
        long timestamp,
        List<Instruction> instructions
) {
    /**
     * Single turn-by-turn navigation instruction.
     *
     * @param turnType  instruction type
     *                  -98 = U-turn (không xác định trái/phải)
     *                  -8  = U-turn trái
     *                  -7  = giữ trái
     *                  -6  = rời khỏi vòng xoay
     *                  -3  = rẽ gấp trái
     *                  -2  = rẽ trái
     *                  -1  = rẽ nhẹ trái
     *                  0   = tiếp tục
     *                  1   = rẽ nhẹ phải
     *                  2   = rẽ phải
     *                  3   = rẽ gấp phải
     *                  4   = kết thúc trước điểm cuối
     *                  5   = hướng dẫn trước điểm via
     *                  6   = hướng dẫn trước khi vào vòng xoay
     *                  7   = giữ phải
     *                  8   = U-turn phải
     * @param name      street/road name
     * @param distance  distance to travel in meters (m)
     * @param time      time in milliseconds (ms)t
     * @param command   Vietnamese navigation command mapped from turnType
     */

    public record Instruction(
            int turnType,
            String name,
            double distance,
            long time,
            String command
    ) {
        /**
         * Get Vietnamese command description for a given turnType.
         */
        
        public static String getTurnCommand(int turnType) {
            return switch (turnType) {
                case -98 -> "U-turn (không xác định)";
                case -8 -> "U-turn trái";
                case -7 -> "Giữ trái";
                case -6 -> "Rời khỏi vòng xoay";
                case -3 -> "Rẽ gấp trái";
                case -2 -> "Rẽ trái";
                case -1 -> "Rẽ nhẹ trái";
                case 0 -> "Tiếp tục";
                case 1 -> "Rẽ nhẹ phải";
                case 2 -> "Rẽ phải";
                case 3 -> "Rẽ gấp phải";
                case 4 -> "Kết thúc";
                case 5 -> "Trước điểm via";
                case 6 -> "Trước khi vào vòng xoay";
                case 7 -> "Giữ phải";
                case 8 -> "U-turn phải";
                default -> "Không xác định";
            };
        }
    }
}
