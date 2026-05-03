package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.model.admin.Staff;
import com.drc.aidbridge.domain.usecase.admin.CreateStaffUseCase;
import com.drc.aidbridge.domain.usecase.admin.ListHubsUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAddStaffViewModel extends BaseViewModel {

    private final MutableLiveData<Long> hubsTrigger = new MutableLiveData<>();
    private final MutableLiveData<CreateStaffParams> createStaffTrigger = new MutableLiveData<>();

    private final LiveData<NetworkResultWrapper<List<Hub>>> hubsResult;
    private final LiveData<NetworkResultWrapper<Staff>> createStaffResult;

    @Inject
    public AdminAddStaffViewModel(ListHubsUseCase listHubsUseCase,
                                  CreateStaffUseCase createStaffUseCase) {
        hubsResult = Transformations.switchMap(hubsTrigger, ignored -> listHubsUseCase.execute());
        createStaffResult = Transformations.switchMap(
                createStaffTrigger,
                params -> createStaffUseCase.execute(
                        params.fullName,
                        params.email,
                        params.phoneNumber,
                        params.password,
                        params.hubId
                )
        );
    }

    public LiveData<NetworkResultWrapper<List<Hub>>> getHubsResult() {
        return hubsResult;
    }

    public LiveData<NetworkResultWrapper<Staff>> getCreateStaffResult() {
        return createStaffResult;
    }

    public void loadHubs() {
        hubsTrigger.setValue(System.currentTimeMillis());
    }

    public void createStaff(String fullName,
                            String email,
                            String phoneNumber,
                            String password,
                            String hubId) {
        createStaffTrigger.setValue(new CreateStaffParams(fullName, email, phoneNumber, password, hubId));
    }

    private static final class CreateStaffParams {
        final String fullName;
        final String email;
        final String phoneNumber;
        final String password;
        final String hubId;

        CreateStaffParams(String fullName,
                          String email,
                          String phoneNumber,
                          String password,
                          String hubId) {
            this.fullName = fullName;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.password = password;
            this.hubId = hubId;
        }
    }
}
