package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.admin.Hub;
import com.drc.aidbridge.domain.usecase.admin.CreateHubUseCase;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminAddHubViewModel extends BaseViewModel {

    private final MutableLiveData<CreateHubParams> createHubTrigger = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<Hub>> createHubResult;

    @Inject
    public AdminAddHubViewModel(CreateHubUseCase createHubUseCase) {
        createHubResult = Transformations.switchMap(
                createHubTrigger,
                params -> createHubUseCase.execute(
                        params.name,
                        params.address,
                        params.phoneNumber,
                        params.imageUrl,
                        params.operatingHours,
                        params.latitude,
                        params.longitude));
    }

    public LiveData<NetworkResultWrapper<Hub>> getCreateHubResult() {
        return createHubResult;
    }

    public void createHub(String name,
            String address,
            String phoneNumber,
            String imageUrl,
            String operatingHours,
            Double latitude,
            Double longitude) {
        createHubTrigger.setValue(new CreateHubParams(
                name,
                address,
                phoneNumber,
                imageUrl,
                operatingHours,
                latitude,
                longitude));
    }

    private static final class CreateHubParams {
        final String name;
        final String address;
        final String phoneNumber;
        final String imageUrl;
        final String operatingHours;
        final Double latitude;
        final Double longitude;

        CreateHubParams(String name,
                String address,
                String phoneNumber,
                String imageUrl,
                String operatingHours,
                Double latitude,
                Double longitude) {
            this.name = name;
            this.address = address;
            this.phoneNumber = phoneNumber;
            this.imageUrl = imageUrl;
            this.operatingHours = operatingHours;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
