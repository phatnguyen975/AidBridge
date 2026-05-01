package com.drc.aidbridge.ui.main.viewmodel.staff;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.staff.InventoryConfirmItem;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreview;
import com.drc.aidbridge.domain.model.staff.InventoryTransactionResult;
import com.drc.aidbridge.domain.model.staff.InboundDonationPreview;
import com.drc.aidbridge.domain.model.staff.InboundDraftItem;
import com.drc.aidbridge.domain.model.staff.InboundSubCategory;
import com.drc.aidbridge.domain.repository.staff.StaffInventoryRepository;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StaffInventoryTransactionViewModel extends BaseViewModel {

    public static final String MODE_IMPORT = "import";
    public static final String MODE_EXPORT = "export";

    private final StaffInventoryRepository repository;
    private final MutableLiveData<PreviewParams> previewParams = new MutableLiveData<>();
    private final MutableLiveData<String> inboundPreviewCode = new MutableLiveData<>();
    private final MutableLiveData<ConfirmParams> confirmParams = new MutableLiveData<>();
    private final MutableLiveData<InboundConfirmParams> inboundConfirmParams = new MutableLiveData<>();
    private final LiveData<NetworkResultWrapper<InventoryQrPreview>> previewResult;
    private final LiveData<NetworkResultWrapper<InboundDonationPreview>> inboundPreviewResult;
    private final LiveData<NetworkResultWrapper<InventoryTransactionResult>> confirmResult;
    private final LiveData<NetworkResultWrapper<InventoryTransactionResult>> inboundConfirmResult;

    @Inject
    public StaffInventoryTransactionViewModel(StaffInventoryRepository repository) {
        this.repository = repository;
        previewResult = Transformations.switchMap(previewParams, params -> {
            if (params == null || params.code.isEmpty()) {
                return emptyPreviewError();
            }
            return repository.previewOutbound(params.code);
        });
        inboundPreviewResult = Transformations.switchMap(inboundPreviewCode, code -> {
            if (code == null || code.isEmpty()) {
                return emptyInboundPreviewError();
            }
            return repository.previewInbound(code);
        });
        confirmResult = Transformations.switchMap(confirmParams, params -> {
            if (params == null || params.code.isEmpty()) {
                return emptyTransactionError();
            }
            return repository.confirmOutbound(params.code, params.items, params.note);
        });
        inboundConfirmResult = Transformations.switchMap(inboundConfirmParams, params -> {
            if (params == null || (params.donationId.isEmpty() && params.code.isEmpty())) {
                return emptyTransactionError();
            }
            return repository.confirmInbound(params.donationId, params.code, params.items, params.generalNote);
        });
    }

    public LiveData<NetworkResultWrapper<InventoryQrPreview>> getPreviewResult() {
        return previewResult;
    }

    public LiveData<NetworkResultWrapper<InventoryTransactionResult>> getConfirmResult() {
        return confirmResult;
    }

    public LiveData<NetworkResultWrapper<InboundDonationPreview>> getInboundPreviewResult() {
        return inboundPreviewResult;
    }

    public LiveData<NetworkResultWrapper<InventoryTransactionResult>> getInboundConfirmResult() {
        return inboundConfirmResult;
    }

    public void preview(String mode, @Nullable String code) {
        previewParams.setValue(new PreviewParams(normalizeMode(mode), trimToEmpty(code)));
    }

    public void previewInbound(@Nullable String code) {
        inboundPreviewCode.setValue(trimToEmpty(code));
    }

    public void confirm(String mode,
                        @Nullable String code,
                        List<InventoryConfirmItem> items,
                        @Nullable String note) {
        confirmParams.setValue(new ConfirmParams(
                normalizeMode(mode),
                trimToEmpty(code),
                items,
                trimToEmpty(note)
        ));
    }

    public void confirmInbound(@Nullable String donationId,
                               @Nullable String code,
                               List<InboundDraftItem> items,
                               @Nullable String generalNote) {
        inboundConfirmParams.setValue(new InboundConfirmParams(
                trimToEmpty(donationId),
                trimToEmpty(code),
                items,
                trimToEmpty(generalNote)
        ));
    }

    public LiveData<NetworkResultWrapper<List<InboundSubCategory>>> searchInboundSubCategories(
            @Nullable String donationId,
            @Nullable String parentCategoryId,
            @Nullable String keyword) {
        return repository.searchInboundSubCategories(
                trimToEmpty(donationId),
                trimToEmpty(parentCategoryId),
                trimToEmpty(keyword)
        );
    }

    public LiveData<NetworkResultWrapper<InboundSubCategory>> createInboundSubCategory(@Nullable String donationId,
                                                                                      @Nullable String parentCategoryId,
                                                                                      @Nullable String name,
                                                                                      @Nullable String unit,
                                                                                      @Nullable String iconUrl) {
        return repository.createInboundSubCategory(
                trimToEmpty(donationId),
                trimToEmpty(parentCategoryId),
                trimToEmpty(name),
                trimToEmpty(unit),
                trimToEmpty(iconUrl)
        );
    }

    private LiveData<NetworkResultWrapper<InventoryQrPreview>> emptyPreviewError() {
        MutableLiveData<NetworkResultWrapper<InventoryQrPreview>> result = new MutableLiveData<>();
        result.setValue(NetworkResultWrapper.error("Vui l\u00f2ng nh\u1eadp m\u00e3."));
        return result;
    }

    private LiveData<NetworkResultWrapper<InboundDonationPreview>> emptyInboundPreviewError() {
        MutableLiveData<NetworkResultWrapper<InboundDonationPreview>> result = new MutableLiveData<>();
        result.setValue(NetworkResultWrapper.error("Vui l\u00f2ng nh\u1eadp m\u00e3."));
        return result;
    }

    private LiveData<NetworkResultWrapper<InventoryTransactionResult>> emptyTransactionError() {
        MutableLiveData<NetworkResultWrapper<InventoryTransactionResult>> result = new MutableLiveData<>();
        result.setValue(NetworkResultWrapper.error("Vui l\u00f2ng nh\u1eadp m\u00e3."));
        return result;
    }

    private String normalizeMode(@Nullable String mode) {
        return MODE_IMPORT.equals(mode) ? MODE_IMPORT : MODE_EXPORT;
    }

    private String trimToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static final class PreviewParams {
        final String mode;
        final String code;

        PreviewParams(String mode, String code) {
            this.mode = mode;
            this.code = code;
        }
    }

    private static final class ConfirmParams {
        final String mode;
        final String code;
        final List<InventoryConfirmItem> items;
        final String note;

        ConfirmParams(String mode, String code, List<InventoryConfirmItem> items, String note) {
            this.mode = mode;
            this.code = code;
            this.items = items;
            this.note = note;
        }
    }

    private static final class InboundConfirmParams {
        final String donationId;
        final String code;
        final List<InboundDraftItem> items;
        final String generalNote;

        InboundConfirmParams(String donationId,
                             String code,
                             List<InboundDraftItem> items,
                             String generalNote) {
            this.donationId = donationId;
            this.code = code;
            this.items = items;
            this.generalNote = generalNote;
        }
    }
}
