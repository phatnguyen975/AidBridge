package com.drc.aidbridge.modules.routing.internal.util;

import com.graphhopper.util.PointList;

/**
 * Encodes a list of lat/lng points into Google Maps Encoded Polyline format.
 * <p>
 * Algorithm reference:
 * <a href="https://developers.google.com/maps/documentation/utilities/polylinealgorithm">
 *   Google Polyline Algorithm</a>
 * <p>
 * Precision: 5 decimal places (standard for Google Maps).
 */
public final class PolylineEncoder {

    private PolylineEncoder() {
    }

    /**
     * Encode a GraphHopper PointList into a Google Maps encoded polyline string.
     */
    public static String encode(PointList points) {
        StringBuilder encoded = new StringBuilder();
        int prevLat = 0;
        int prevLng = 0;

        for (int i = 0; i < points.size(); i++) {
            int lat = (int) Math.round(points.getLat(i) * 1e5);
            int lng = (int) Math.round(points.getLon(i) * 1e5);

            encodeSignedNumber(lat - prevLat, encoded);
            encodeSignedNumber(lng - prevLng, encoded);

            prevLat = lat;
            prevLng = lng;
        }

        return encoded.toString();
    }

    private static void encodeSignedNumber(int num, StringBuilder buf) {
        int sgn = num << 1;
        if (num < 0) {
            sgn = ~sgn;
        }
        while (sgn >= 0x20) {
            buf.append((char) ((0x20 | (sgn & 0x1f)) + 63));
            sgn >>= 5;
        }
        buf.append((char) (sgn + 63));
    }
}
