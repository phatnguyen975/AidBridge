package com.drc.aidbridge.sms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SosSmsFormatterParserTest {

    @Test
    public void formatQuickSosAndParse_ShouldRoundTripRequiredFields() {
        SosSmsFormatter formatter = new SosSmsFormatter();
        SosSmsParser parser = new SosSmsParser();

        String body = formatter.formatQuickSos(
            "TEST-123",
            10.762622,
            106.660172,
            12.5,
            1710000000000L,
            1,
            true,
            "0901234567",
            null
        );

        assertTrue(body.startsWith("AIDBRIDGE_SOS|"));
        assertTrue(body.contains("|id=TEST-123"));
        assertTrue(body.contains("|lat=10.762622"));
        assertTrue(body.contains("|lng=106.660172"));
        assertTrue(body.contains("|acc=12.5"));
        assertTrue(body.contains("|t=1710000000000"));
        assertTrue(body.contains("|n=1"));
        assertTrue(body.contains("|q=1"));
        assertTrue(body.contains("|p=0901234567"));

        GatewaySmsSosPayload payload = parser.parse("sender", body, 1710000000100L);

        assertNotNull(payload);
        assertEquals("TEST-123", payload.getClientRequestId());
        assertEquals("0901234567", payload.getSenderPhone());
        assertEquals(10.762622, payload.getLatitude(), 0.000001);
        assertEquals(106.660172, payload.getLongitude(), 0.000001);
        assertEquals(12.5, payload.getAccuracy(), 0.000001);
        assertEquals(1710000000000L, payload.getTriggeredAtMillis());
        assertEquals(1, payload.getPeopleCount());
        assertTrue(payload.isQuickSos());
        assertEquals(body, payload.getRawMessage());
        assertEquals(1710000000100L, payload.getReceivedAtGatewayMillis());
    }

    @Test
    public void parse_ShouldRejectMissingRequiredFieldsAndInvalidCoordinates() {
        SosSmsParser parser = new SosSmsParser();

        assertNull(parser.parse("sender", "hello", 1L));
        assertNull(parser.parse("sender", "AIDBRIDGE_SOS|lat=10|lng=106", 1L));
        assertNull(parser.parse("sender", "AIDBRIDGE_SOS|id=TEST|lng=106", 1L));
        assertNull(parser.parse("sender", "AIDBRIDGE_SOS|id=TEST|lat=10", 1L));
        assertNull(parser.parse("sender", "AIDBRIDGE_SOS|id=TEST|lat=91|lng=106", 1L));
        assertNull(parser.parse("sender", "AIDBRIDGE_SOS|id=TEST|lat=10|lng=181", 1L));
    }

    @Test
    public void parse_ShouldAllowExplicitNonQuickFlag() {
        SosSmsParser parser = new SosSmsParser();

        GatewaySmsSosPayload payload = parser.parse(
            "0901234567",
            "AIDBRIDGE_SOS|id=TEST-123|lat=10.762622|lng=106.660172|t=1710000000000|n=2|q=0",
            1710000000100L
        );

        assertNotNull(payload);
        assertFalse(payload.isQuickSos());
        assertEquals(2, payload.getPeopleCount());
        assertEquals("0901234567", payload.getSenderPhone());
    }
}
