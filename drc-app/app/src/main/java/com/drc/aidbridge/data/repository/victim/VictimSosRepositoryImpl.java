package com.drc.aidbridge.data.repository.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.dto.request.victim.CreateSosRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.victim.SosRequestResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;
import com.drc.aidbridge.utils.TokenManager;

import java.text.Normalizer;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VictimSosRepositoryImpl extends BaseRepository implements VictimSosRepository {

    private final SosApiService sosApiService;
    private final TokenManager tokenManager;

    @Inject
    public VictimSosRepositoryImpl(SosApiService sosApiService,
                                   TokenManager tokenManager) {
        this.sosApiService = sosApiService;
        this.tokenManager = tokenManager;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> uploadSelfSos(String fullName,
                                                                 int peopleCount,
                                                                 String severity,
                                                                 String note,
                                                                 double latitude,
                                                                 double longitude,
                                                                 String firstImageUrl) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String accountName = firstNonBlank(safeTrim(tokenManager.getUserName()), safeTrim(fullName));
        String accountPhone = safeTrim(tokenManager.getUserPhone());
        String description = buildSelfDescription(accountName, accountPhone, note);

        CreateSosRequest request = new CreateSosRequest(
            latitude,
            longitude,
            null,
            description,
            peopleCount,
            mapSeverityToUrgencyLevel(severity),
            safeTrim(firstImageUrl)
        );

        sosApiService.createSosRequest(request).enqueue(new Callback<BaseResponse<SosRequestResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SosRequestResponse>> call,
                                   Response<BaseResponse<SosRequestResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SosRequestResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi SOS không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(
                        firstNonBlank(baseResponse.getMessage(), "Gửi SOS thất bại.")
                    ));
                    return;
                }

                String successMessage = firstNonBlank(
                    baseResponse.getMessage(),
                    "Gửi SOS thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<SosRequestResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error("Gửi SOS thất bại: " + safeMessage(t)));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<String>> uploadRelativeSos(String relativeName,
                                                                     String relativePhone,
                                                                     String relativeAddress,
                                                                     String severity,
                                                                     double latitude,
                                                                     double longitude) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        String description = buildRelativeDescription(relativeName, relativePhone);
        CreateSosRequest request = new CreateSosRequest(
            latitude,
            longitude,
            safeTrim(relativeAddress),
            description,
            1,
            mapSeverityToUrgencyLevel(severity),
            null
        );

        sosApiService.createSosRequest(request).enqueue(new Callback<BaseResponse<SosRequestResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<SosRequestResponse>> call,
                                   Response<BaseResponse<SosRequestResponse>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<SosRequestResponse> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi gửi SOS cho người thân không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    result.postValue(NetworkResultWrapper.error(
                        firstNonBlank(baseResponse.getMessage(), "Gửi SOS cho người thân thất bại.")
                    ));
                    return;
                }

                String successMessage = firstNonBlank(
                    baseResponse.getMessage(),
                    "Gửi SOS cho người thân thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<SosRequestResponse>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error(
                    "Gửi SOS cho người thân thất bại: " + safeMessage(t)
                ));
            }
        });

        return result;
    }

    private String buildRelativeDescription(String relativeName, String relativePhone) {
        String name = firstNonBlank(safeTrim(relativeName), "Không có");
        String phone = firstNonBlank(safeTrim(relativePhone), "Không có");
        return "Loại yêu cầu: SOS người thân. Họ tên: " + name + ". Số điện thoại liên hệ: " + phone;
    }

    private String buildSelfDescription(String fullName, String phoneNumber, String healthDetail) {
        String name = firstNonBlank(safeTrim(fullName), "Không có");
        String phone = firstNonBlank(safeTrim(phoneNumber), "Không có");
        String health = firstNonBlank(safeTrim(healthDetail), "Không có");

        return "Loại yêu cầu: SOS bản thân. Họ tên: " + name
            + ". Số điện thoại liên hệ: " + phone
            + ". Chi tiết sức khỏe: " + health;
    }

    private String mapSeverityToUrgencyLevel(String severity) {
        String value = normalizeSeverityText(severity);
        if (value.isEmpty()) {
            return "CRITICAL";
        }

        if ("CRITICAL".equals(value)
            || "NGUY KICH".equals(value)
            || "NGUYKICH".equals(value)) {
            return "CRITICAL";
        }
        if ("HIGH".equals(value)
            || "NGHIEM TRONG".equals(value)
            || "NGHIEMTRONG".equals(value)) {
            return "HIGH";
        }
        if ("LOW".equals(value) || "NHE".equals(value)) {
            return "LOW";
        }
        if ("MEDIUM".equals(value)
            || "TRUNG BINH".equals(value)
            || "TRUNGBINH".equals(value)) {
            return "MEDIUM";
        }

        return "CRITICAL";
    }

    private String normalizeSeverityText(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.US)
            .trim();

        return normalized;
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
