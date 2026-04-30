package com.drc.aidbridge.data.repository.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.api.admin.AdminStaffApiService;
import com.drc.aidbridge.data.remote.dto.request.admin.CreateStaffRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.data.remote.dto.response.admin.StaffResponseDto;
import com.drc.aidbridge.data.repository.BaseRepository;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.domain.repository.admin.AdminStaffRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class AdminStaffRepositoryImpl extends BaseRepository implements AdminStaffRepository {

    private final AdminStaffApiService apiService;

    @Inject
    public AdminStaffRepositoryImpl(AdminStaffApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public LiveData<NetworkResultWrapper<List<Staff>>> getStaffList() {
        MutableLiveData<NetworkResultWrapper<List<Staff>>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        apiService.getStaffList().enqueue(new Callback<BaseResponse<List<StaffResponseDto>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<StaffResponseDto>>> call,
                                   Response<BaseResponse<List<StaffResponseDto>>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(
                            resolveHttpError(response, "Bạn không có quyền xem danh sách Staff"),
                            response.code()));
                    return;
                }

                BaseResponse<List<StaffResponseDto>> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi danh sách Staff không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String message = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không thể tải danh sách Staff."));
                    return;
                }

                List<Staff> mapped = new ArrayList<>();
                List<StaffResponseDto> data = baseResponse.getData();
                if (data != null) {
                    for (StaffResponseDto dto : data) {
                        Staff staff = mapToDomain(dto);
                        if (staff != null) {
                            mapped.add(staff);
                        }
                    }
                }
                result.postValue(NetworkResultWrapper.success(mapped));
            }

            @Override
            public void onFailure(Call<BaseResponse<List<StaffResponseDto>>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ"));
            }
        });

        return result;
    }

    @Override
    public LiveData<NetworkResultWrapper<Staff>> createStaff(String fullName,
                                                             String email,
                                                             String phoneNumber,
                                                             String password,
                                                             String hubId) {
        MutableLiveData<NetworkResultWrapper<Staff>> result = new MutableLiveData<>();
        result.postValue(NetworkResultWrapper.loading());

        CreateStaffRequest request = new CreateStaffRequest(fullName, email, phoneNumber, password, hubId);
        apiService.createStaff(request).enqueue(new Callback<BaseResponse<StaffResponseDto>>() {
            @Override
            public void onResponse(Call<BaseResponse<StaffResponseDto>> call,
                                   Response<BaseResponse<StaffResponseDto>> response) {
                if (!response.isSuccessful()) {
                    result.postValue(NetworkResultWrapper.error(
                            resolveHttpError(response, "Bạn không có quyền tạo Staff"),
                            response.code()));
                    return;
                }

                BaseResponse<StaffResponseDto> baseResponse = response.body();
                if (baseResponse == null) {
                    result.postValue(NetworkResultWrapper.error("Phản hồi tạo Staff không hợp lệ."));
                    return;
                }

                if (!baseResponse.isSuccess()) {
                    String message = baseResponse.getMessage();
                    result.postValue(NetworkResultWrapper.error(
                            message != null && !message.trim().isEmpty()
                                    ? message
                                    : "Không thể tạo Staff."));
                    return;
                }

                Staff staff = mapToDomain(baseResponse.getData());
                if (staff == null) {
                    result.postValue(NetworkResultWrapper.error("Dữ liệu Staff vừa tạo không hợp lệ."));
                    return;
                }
                result.postValue(NetworkResultWrapper.success(staff));
            }

            @Override
            public void onFailure(Call<BaseResponse<StaffResponseDto>> call, Throwable throwable) {
                result.postValue(NetworkResultWrapper.error("Không thể kết nối máy chủ"));
            }
        });

        return result;
    }

    private String resolveHttpError(Response<?> response, String forbiddenMessage) {
        if (response.code() == 403) {
            return forbiddenMessage;
        }
        return extractHttpError(response);
    }

    private Staff mapToDomain(StaffResponseDto dto) {
        if (dto == null) {
            return null;
        }
        return new Staff(
                dto.getId(),
                dto.getUserId(),
                dto.getFullName(),
                dto.getEmail(),
                dto.getPhoneNumber(),
                dto.getHubId(),
                dto.getHubName()
        );
    }
}
