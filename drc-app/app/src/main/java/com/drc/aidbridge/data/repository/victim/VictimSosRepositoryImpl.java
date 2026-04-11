package com.drc.aidbridge.data.repository.victim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.repository.victim.VictimSosRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VictimSosRepositoryImpl extends BaseRepository implements VictimSosRepository {

    private static final MediaType TEXT_PLAIN = MediaType.get("text/plain");

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
                                                                 List<MultipartBody.Part> imageParts) {
        MutableLiveData<NetworkResultWrapper<String>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        sosApiService.uploadSelfSos(
            toPart(fullName),
            toPart(String.valueOf(peopleCount)),
            toPart(severity),
            toPart(note),
            toPart(String.valueOf(latitude)),
            toPart(String.valueOf(longitude)),
            imageParts
        ).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                   Response<BaseResponse<String>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<String> baseResponse = response.body();
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
                    baseResponse.getData(),
                    baseResponse.getMessage(),
                    "Gửi SOS thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
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

        sosApiService.uploadRelativeSos(
            toPart(relativeName),
            toPart(relativePhone),
            toPart(relativeAddress),
            toPart(severity),
            toPart(String.valueOf(latitude)),
            toPart(String.valueOf(longitude))
        ).enqueue(new Callback<BaseResponse<String>>() {
            @Override
            public void onResponse(Call<BaseResponse<String>> call,
                                   Response<BaseResponse<String>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(extractHttpError(response), response.code()));
                    return;
                }

                BaseResponse<String> baseResponse = response.body();
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
                    baseResponse.getData(),
                    baseResponse.getMessage(),
                    "Gửi SOS cho người thân thành công."
                );
                result.postValue(NetworkResultWrapper.success(successMessage));
            }

            @Override
            public void onFailure(Call<BaseResponse<String>> call, Throwable t) {
                result.postValue(NetworkResultWrapper.error(
                    "Gửi SOS cho người thân thất bại: " + safeMessage(t)
                ));
            }
        });

        return result;
    }

    private RequestBody toPart(String value) {
        return RequestBody.create(value != null ? value.trim() : "", TEXT_PLAIN);
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
