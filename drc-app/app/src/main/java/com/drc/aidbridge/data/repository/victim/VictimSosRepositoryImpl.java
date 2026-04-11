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

    @Inject
    public VictimSosRepositoryImpl(SosApiService sosApiService) {
        this.sosApiService = sosApiService;
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

        CreateSosRequest request = new CreateSosRequest(
            latitude,
            longitude,
            null,
            safeTrim(note),
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
        String name = safeTrim(relativeName);
        String phone = safeTrim(relativePhone);
        return firstNonBlank(
            (name.isEmpty() && phone.isEmpty())
                ? "Yeu cau SOS cho nguoi than"
                : ("Ho ten: " + name + ". So dien thoai lien he: " + phone),
            "Yeu cau SOS cho nguoi than"
        );
    }

    private String mapSeverityToUrgencyLevel(String severity) {
        String value = normalizeSeverityText(severity);
        if (value.isEmpty()) {
            return "CRITICAL";
        }

        if ("CRITICAL".equals(value) || value.contains("NGUY") || value.contains("KICH")) {
            return "CRITICAL";
        }
        if ("HIGH".equals(value) || value.contains("NGHIEM") || value.contains("TRONG")) {
            return "HIGH";
        }
        if ("LOW".equals(value) || value.contains("NHE")) {
            return "LOW";
        }
        if ("MEDIUM".equals(value) || value.contains("TRUNG") || value.contains("BINH")) {
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
