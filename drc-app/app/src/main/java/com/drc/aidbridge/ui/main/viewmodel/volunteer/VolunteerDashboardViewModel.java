package com.drc.aidbridge.ui.main.viewmodel.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.ui.base.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VolunteerDashboardViewModel extends BaseViewModel {

    private final MutableLiveData<Boolean> _isOnline = new MutableLiveData<>(false);
    public LiveData<Boolean> isOnline() { return _isOnline; }

    private final MutableLiveData<NetworkResultWrapper<Boolean>> _updateStatusResult = new MutableLiveData<>();
    public LiveData<NetworkResultWrapper<Boolean>> getUpdateStatusResult() { return _updateStatusResult; }

    @Inject
    public VolunteerDashboardViewModel() {
        // Khởi tạo các UseCase cần thiết ở đây
    }

    public void toggleOnlineStatus(boolean online) {
        _updateStatusResult.setValue(NetworkResultWrapper.loading());
        
        // Giả định gọi UseCase cập nhật trạng thái
        // Trong thực tế sẽ gọi: updateStatusUseCase.execute(online)...
        
        // Mock delay và kết quả
        new android.os.Handler().postDelayed(() -> {
            _isOnline.setValue(online);
            _updateStatusResult.setValue(NetworkResultWrapper.success(online));
        }, 1000);
    }
}
