package com.drc.aidbridge.domain.model.sponsor;

public class SponsorDonationSubmissionResult {

    private final String message;
    private final String donationCode;
    private final String qrCodeToken;

    public SponsorDonationSubmissionResult(String message, String donationCode, String qrCodeToken) {
        this.message = message;
        this.donationCode = donationCode;
        this.qrCodeToken = qrCodeToken;
    }

    public String getMessage() {
        return message;
    }

    public String getDonationCode() {
        return donationCode;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }
}
