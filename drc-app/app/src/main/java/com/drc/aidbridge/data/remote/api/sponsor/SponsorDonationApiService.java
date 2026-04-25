package com.drc.aidbridge.data.remote.api.sponsor;

import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationQrResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;

public interface SponsorDonationApiService {

    @POST("donations")
    Call<BaseResponse<SponsorDonationResponse>> createDonation(@Body CreateDonationRequest request);

    @GET("donations/{id}/qr")
    Call<BaseResponse<SponsorDonationQrResponse>> getDonationQr(@Path("id") String donationId);
}
