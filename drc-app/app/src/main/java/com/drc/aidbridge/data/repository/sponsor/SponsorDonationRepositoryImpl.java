package com.drc.aidbridge.data.repository.sponsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.mapper.sponsor.SponsorDonationMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.sponsor.SponsorDonationApiService;
import com.drc.aidbridge.data.remote.dto.request.sponsor.CreateDonationRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationHistoryDataResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationHistoryItemResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationHistoryPaginationResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationHistorySelectedItemResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationQrResponse;
import com.drc.aidbridge.data.remote.dto.response.sponsor.SponsorDonationResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryItem;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationHistoryPage;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationRequest;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationStatus;
import com.drc.aidbridge.domain.model.sponsor.SponsorDonationSubmissionResult;
import com.drc.aidbridge.domain.repository.sponsor.SponsorDonationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
    public LiveData<NetworkResultWrapper<SponsorDonationSubmissionResult>> submitDonation(SponsorDonationRequest request) {
        MutableLiveData<NetworkResultWrapper<SponsorDonationSubmissionResult>> result = new MutableLiveData<>();
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

                SponsorDonationResponse createData = body.getData();
                String donationId = createData != null ? safeText(createData.getId()) : "";
                if (donationId.isEmpty()) {
                    result.postValue(NetworkResultWrapper.error("Không lấy được mã donation sau khi tạo."));
                    return;
                }

                String message = body.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Gửi đăng ký đóng góp thành công.";
                }

                fetchDonationQr(result, donationId, message.trim(), createData);
            }

            @Override
            public void onFailure(Call<BaseResponse<SponsorDonationResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi đăng ký đóng góp thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> getDonationHistory(String status, int page, int limit) {
        MutableLiveData<NetworkResultWrapper<SponsorDonationHistoryPage>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        sponsorDonationApiService.getDonationHistory(normalizeStatusQuery(status), page, limit)
                .enqueue(new Callback<BaseResponse<SponsorDonationHistoryDataResponse>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<SponsorDonationHistoryDataResponse>> call,
                                           Response<BaseResponse<SponsorDonationHistoryDataResponse>> response) {
                        if (!response.isSuccessful()) {
                            result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                            return;
                        }

                        BaseResponse<SponsorDonationHistoryDataResponse> body = response.body();
                        if (body == null) {
                            result.postValue(NetworkResultWrapper.error("Phản hồi lịch sử đóng góp không hợp lệ."));
                            return;
                        }

                        if (!body.isSuccess()) {
                            String message = body.getMessage();
                            result.postValue(NetworkResultWrapper.error(
                                    message != null && !message.trim().isEmpty()
                                            ? message.trim()
                                            : "Không thể tải lịch sử đóng góp."
                            ));
                            return;
                        }

                        SponsorDonationHistoryDataResponse data = body.getData();
                        if (data == null) {
                            result.postValue(NetworkResultWrapper.error("Dữ liệu lịch sử đóng góp trống."));
                            return;
                        }

                        List<SponsorDonationHistoryItem> mappedItems = mapHistoryItems(data.getItems());
                        SponsorDonationHistoryPaginationResponse pagination = data.getPagination();

                        SponsorDonationHistoryPage pageData = new SponsorDonationHistoryPage(
                                mappedItems,
                                pagination != null ? pagination.getPage() : page,
                                pagination != null ? pagination.getLimit() : limit,
                                pagination != null ? pagination.getTotal() : mappedItems.size(),
                                pagination != null ? pagination.getTotalPages() : 1,
                                pagination != null && pagination.isHasNext()
                        );
                        result.postValue(NetworkResultWrapper.success(pageData));
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<SponsorDonationHistoryDataResponse>> call, Throwable t) {
                        result.postValue(NetworkResultWrapper.error("Không thể tải lịch sử đóng góp: " + safeMessage(t)));
                    }
                });

        return result;
    }

    private void fetchDonationQr(MutableLiveData<NetworkResultWrapper<SponsorDonationSubmissionResult>> result,
                                 String donationId,
                                 String submitMessage,
                                 SponsorDonationResponse createData) {
        sponsorDonationApiService.getDonationQr(donationId).enqueue(new Callback<BaseResponse<SponsorDonationQrResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SponsorDonationQrResponse>> call,
                                   Response<BaseResponse<SponsorDonationQrResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SponsorDonationQrResponse> body = response.body();
                if (body == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi lấy mã QR không hợp lệ."));
                    return;
                }

                if (!body.isSuccess()) {
                    String message = body.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                        message != null && !message.trim().isEmpty()
                            ? message.trim()
                            : "Không thể lấy mã QR donation."
                    ));
                    return;
                }

                SponsorDonationQrResponse qrData = body.getData();
                String qrToken = qrData != null ? safeText(qrData.getQrCodeToken()) : "";
                if (qrToken.isEmpty() && createData != null) {
                    qrToken = safeText(createData.getQrCodeToken());
                }

                if (qrToken.isEmpty()) {
                    result.postValue(NetworkResultWrapper.error("Không tìm thấy mã QR donation."));
                    return;
                }
                String donationCode = qrData != null ? safeText(qrData.getDonationCode()) : "";
                result.postValue(NetworkResultWrapper.success(
                    new SponsorDonationSubmissionResult(submitMessage, donationCode, qrToken)
                ));
            }

            @Override
            public void onFailure(Call<BaseResponse<SponsorDonationQrResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Lấy mã QR donation thất bại: " + safeMessage(t)));
            }
        });
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private String normalizeStatusQuery(String status) {
        String normalized = safeText(status);
        return normalized.isEmpty() ? null : normalized;
    }

    private List<SponsorDonationHistoryItem> mapHistoryItems(List<SponsorDonationHistoryItemResponse> apiItems) {
        List<SponsorDonationHistoryItem> mapped = new ArrayList<>();
        if (apiItems == null || apiItems.isEmpty()) {
            return mapped;
        }

        for (SponsorDonationHistoryItemResponse apiItem : apiItems) {
            if (apiItem == null) {
                continue;
            }

            SponsorDonationStatus status = SponsorDonationStatus.fromApiValue(apiItem.getStatus());
            if (status == null) {
                continue;
            }

            int itemCount = apiItem.getItems() != null ? apiItem.getItems().size() : 0;
            String itemSummary = buildItemSummary(apiItem.getItems());

            mapped.add(new SponsorDonationHistoryItem(
                    safeText(apiItem.getId()),
                    safeText(apiItem.getDonationCode()),
                    safeText(apiItem.getQrCodeToken()),
                    status,
                    safeText(apiItem.getCreatedAt()),
                    safeText(apiItem.getHubId()),
                    itemCount,
                    itemSummary
            ));
        }

        return mapped;
    }

    private String buildItemSummary(List<SponsorDonationHistorySelectedItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (SponsorDonationHistorySelectedItemResponse item : items) {
            if (item == null) {
                continue;
            }

            String name = safeText(item.getItemCategoryName());
            if (name.isEmpty()) {
                continue;
            }

            joiner.add(name);
            if (joiner.length() > 120) {
                break;
            }
        }

        return joiner.toString();
    }
}
