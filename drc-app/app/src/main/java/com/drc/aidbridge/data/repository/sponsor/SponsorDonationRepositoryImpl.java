package com.drc.aidbridge.data.repository.sponsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.sponsor.SponsorDonationMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.sponsor.SponsorDonationApiService;
import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.repository.sponsor.SponsorDonationRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class SponsorDonationRepositoryImpl extends BaseRepository implements SponsorDonationRepository {

    private final SponsorDonationApiService sponsorDonationApiService;
    private final SponsorDonationMapper sponsorDonationMapper;

    @Inject
    public SponsorDonationRepositoryImpl(SponsorDonationApiService sponsorDonationApiService,
                                         SponsorDonationMapper sponsorDonationMapper) {
        this.sponsorDonationApiService = sponsorDonationApiService;
        this.sponsorDonationMapper = sponsorDonationMapper;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> submitDonation(SponsorDonationRequest request) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        CreateDonationRequest apiRequest = sponsorDonationMapper.mapToApiRequest(request);

        sponsorDonationApiService.createDonation(apiRequest).enqueue(new Callback<BaseResponse<SponsorDonationResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SponsorDonationResponse>> call,
                                   Response<BaseResponse<SponsorDonationResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SponsorDonationResponse> body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi đóng góp không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        message != null && !message.trim().isEmpty()
                            ? message.trim()
                            : "Không thể gửi đăng ký đóng góp."
                    ));
                    return;
                }

                String message = body.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Gửi đăng ký đóng góp thành công.";
                }

                result.postValue(NetworkResultWrapper.success(message.trim()));
            }

            @Override
            public void onFailure(Call<BaseResponse<SponsorDonationResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi đăng ký đóng góp thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }
}
