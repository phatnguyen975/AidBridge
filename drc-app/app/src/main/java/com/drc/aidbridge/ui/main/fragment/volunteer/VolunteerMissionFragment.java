package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.drc.aidbridge.data.remote.dto.response.volunteer.LatestDispatchDataDto;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerMissionBinding;
import com.drc.aidbridge.domain.model.VolunteerMission;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerTaskViewModel;

import com.drc.aidbridge.ui.main.adapter.volunteer.VolunteerMissionHistoryFullAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerMissionFragment extends BaseFragment<FragmentVolunteerMissionBinding> {

    private VolunteerTaskViewModel volunteerTaskViewModel;
    private Integer lastRoutedId;
    private android.animation.ValueAnimator strokeAnimator;

    @Inject
    VolunteerMissionHistoryFullAdapter volunteerMissionHistoryFullAdapter;

    private int currentPage = 1;
    private static final int PAGE_LIMIT = 10;
    private String currentMissionId = null;

    @Nullable
    @Override
    protected FragmentVolunteerMissionBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVolunteerMissionBinding.inflate(inflater, container, false);
    }

    @Override
    public void onDestroyView() {
        stopGlowAnimation();
        if (binding != null && binding.cardSosAlert != null) {
            binding.cardSosAlert.clearAnimation();
        }
        super.onDestroyView();
    }

    @Override
    protected void setupViews() {
        volunteerTaskViewModel = new ViewModelProvider(requireActivity()).get(VolunteerTaskViewModel.class);
        
        binding.rvMissionHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMissionHistory.setAdapter(volunteerMissionHistoryFullAdapter);
        volunteerMissionHistoryFullAdapter.setOnItemClickListener(this::showMissionDetailPopup);

        showRouterLoading(true);
        evaluateMissionRouter();

        binding.btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                binding.tvCurrentPage.setText("Trang " + currentPage);
                volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
            }
        });

        binding.btnNextPage.setOnClickListener(v -> {
            currentPage++;
            binding.tvCurrentPage.setText("Trang " + currentPage);
            volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
        });
        volunteerTaskViewModel.fetchLatestDispatch();
        volunteerTaskViewModel.fetchCurrentMission();

        binding.btnCompleteCurrentMission.setOnClickListener(v -> {
            if (currentMissionId == null) {
                android.widget.Toast.makeText(requireContext(), "Không tìm thấy mã nhiệm vụ hiện tại.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hoàn thành nhiệm vụ")
                    .setMessage("Bạn có chắc chắn muốn hoàn thành nhiệm vụ hiện tại không?")
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        volunteerTaskViewModel.completeMission(currentMissionId, "Tình nguyện viên báo cáo hoàn thành.");
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
        });

        binding.btnCancelCurrentMission.setOnClickListener(v -> {
            if (currentMissionId == null) {
                android.widget.Toast.makeText(requireContext(), "Không tìm thấy mã nhiệm vụ hiện tại.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hủy nhiệm vụ")
                    .setMessage("Bạn có chắc chắn muốn hủy nhiệm vụ hiện tại không?")
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        volunteerTaskViewModel.cancelMission(currentMissionId, "Tình nguyện viên yêu cầu hủy.");
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
        });

        binding.btnDeclineSos.setOnClickListener(v -> {
            LatestDispatchDataDto currentDispatch = volunteerTaskViewModel.getLatestDispatch().getValue();
            if (currentDispatch != null && currentDispatch.getId() != null) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có chắc chắn muốn hủy tín hiệu SOS này không?")
                        .setPositiveButton("Xác nhận", (dialog, which) -> {
                            volunteerTaskViewModel.cancelDispatch(currentDispatch.getId());
                        })
                        .setNegativeButton("Hủy bỏ", null)
                        .show();
            }
        });

        binding.btnAcceptSos.setOnClickListener(v -> {
            LatestDispatchDataDto currentDispatch = volunteerTaskViewModel.getLatestDispatch().getValue();
            if (currentDispatch != null && currentDispatch.getId() != null) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có chắc chắn muốn nhận tín hiệu SOS này không?")
                        .setPositiveButton("Xác nhận", (dialog, which) -> {
                            volunteerTaskViewModel.acceptDispatch(currentDispatch.getId());
                        })
                        .setNegativeButton("Hủy bỏ", null)
                        .show();
            }
        });
    }

    @Override
    protected void observeViewModel() {
        volunteerTaskViewModel.getIsMissionAccepted().observe(getViewLifecycleOwner(),
                isAccepted -> evaluateMissionRouter());
        volunteerTaskViewModel.getCurrentMissionType().observe(getViewLifecycleOwner(),
                missionType -> evaluateMissionRouter());
        volunteerTaskViewModel.getPendingMission().observe(getViewLifecycleOwner(),
                mission -> evaluateMissionRouter());

        volunteerTaskViewModel.getMissionHistoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullDataDto data = result.getData();
                if (data != null && data.getItems() != null && !data.getItems().isEmpty()) {
                    volunteerMissionHistoryFullAdapter.setItems(data.getItems());
                    binding.rvMissionHistory.setVisibility(View.VISIBLE);
                    binding.tvMissionPlaceholder.setVisibility(View.GONE);
                    binding.tvMissionRouterHint.setVisibility(View.GONE);

                    // Toggle pagination state
                    binding.btnPrevPage.setEnabled(currentPage > 1);
                    binding.btnNextPage.setEnabled(data.getItems().size() >= PAGE_LIMIT);
                } else {
                    volunteerMissionHistoryFullAdapter.setItems(new java.util.ArrayList<>());
                    binding.rvMissionHistory.setVisibility(View.GONE);
                    binding.tvMissionPlaceholder.setVisibility(View.VISIBLE);
                    binding.tvMissionRouterHint.setVisibility(View.GONE);
                    
                    binding.btnPrevPage.setEnabled(currentPage > 1);
                    binding.btnNextPage.setEnabled(false);
                }
            } else if (result != null && result.isError()) {
                binding.rvMissionHistory.setVisibility(View.GONE);
                binding.tvMissionPlaceholder.setVisibility(View.VISIBLE);
                binding.tvMissionRouterHint.setVisibility(View.GONE);
                
                binding.btnPrevPage.setEnabled(currentPage > 1);
                binding.btnNextPage.setEnabled(false);
            }
        });

        volunteerTaskViewModel.getCurrentMissionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto data = result.getData();
                if (data != null) {
                    currentMissionId = data.getId() != null ? data.getId().toString() : null;
                    binding.cardCurrentMission.setVisibility(View.VISIBLE);
                    binding.tvCurrentMissionType.setText("Loại: " + ("RESCUE".equalsIgnoreCase(data.getMissionType()) ? "Cứu trợ khẩn cấp (SOS)" : "Tiếp tế hàng hóa"));
                    binding.tvCurrentMissionAddress.setText("Địa chỉ: " + (data.getAddress() != null ? data.getAddress() : "N/A"));
                    
                    if (data.getVictimLat() != null && data.getVictimLng() != null) {
                        binding.tvCurrentMissionLocation.setText(String.format("Tọa độ: %.6f, %.6f", data.getVictimLat(), data.getVictimLng()));
                    } else if (data.getSosRequestDetail() != null && data.getSosRequestDetail().getLat() != null) {
                        binding.tvCurrentMissionLocation.setText(String.format("Tọa độ: %.6f, %.6f", data.getSosRequestDetail().getLat(), data.getSosRequestDetail().getLng()));
                    } else {
                        binding.tvCurrentMissionLocation.setText("Tọa độ: N/A");
                    }

                    if (data.getRadiusKm() != null) {
                        binding.tvCurrentMissionRadius.setText(String.format("Bán kính cứu trợ: %.2f km", data.getRadiusKm()));
                    } else {
                        binding.tvCurrentMissionRadius.setText("Bán kính cứu trợ: N/A");
                    }
                } else {
                    currentMissionId = null;
                    binding.cardCurrentMission.setVisibility(View.GONE);
                }
            } else if (result != null && result.isError()) {
                currentMissionId = null;
                binding.cardCurrentMission.setVisibility(View.GONE);
            }
        });

        volunteerTaskViewModel.getCompleteMissionApiResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                binding.cardCurrentMission.setVisibility(View.GONE);
                currentMissionId = null;
                volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
                android.widget.Toast.makeText(requireContext(), "Hoàn thành nhiệm vụ thành công!", android.widget.Toast.LENGTH_SHORT).show();
            } else if (result != null && result.isError()) {
                android.widget.Toast.makeText(requireContext(), "Lỗi: " + result.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        });

        volunteerTaskViewModel.getCancelMissionApiResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                binding.cardCurrentMission.setVisibility(View.GONE);
                currentMissionId = null;
                volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
                android.widget.Toast.makeText(requireContext(), "Hủy nhiệm vụ thành công!", android.widget.Toast.LENGTH_SHORT).show();
            } else if (result != null && result.isError()) {
                android.widget.Toast.makeText(requireContext(), "Lỗi: " + result.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        });

        volunteerTaskViewModel.getLatestDispatchResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                LatestDispatchDataDto dispatch = result.getData();
                if (dispatch != null && "PENDING".equalsIgnoreCase(dispatch.getResponse())) {
                    binding.cardSosAlert.setVisibility(View.VISIBLE);
                    binding.tvSosDistance.setText(String.format(java.util.Locale.getDefault(), "Khoảng cách: %.2f km", dispatch.getRadiusKm()));
                    android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(requireContext(), com.drc.aidbridge.R.anim.pulse);
                    binding.cardSosAlert.startAnimation(pulse);
                    startGlowAnimation();
                } else {
                    binding.cardSosAlert.clearAnimation();
                    stopGlowAnimation();
                    binding.cardSosAlert.setVisibility(View.GONE);
                }
            } else if (result != null && result.isError()) {
                binding.cardSosAlert.clearAnimation();
                stopGlowAnimation();
                binding.cardSosAlert.setVisibility(View.GONE);
            }
        });
        volunteerTaskViewModel.getCancelDispatchResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                binding.cardSosAlert.clearAnimation();
                stopGlowAnimation();
                binding.cardSosAlert.setVisibility(View.GONE);
                android.widget.Toast.makeText(requireContext(), "Đã hủy tín hiệu SOS thành công.", android.widget.Toast.LENGTH_SHORT).show();
            } else if (result != null && result.isError()) {
                android.widget.Toast.makeText(requireContext(), "Hủy thất bại: " + result.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        volunteerTaskViewModel.getAcceptDispatchResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                binding.cardSosAlert.clearAnimation();
                stopGlowAnimation();
                binding.cardSosAlert.setVisibility(View.GONE);
                android.widget.Toast.makeText(requireContext(), "Đã nhận tín hiệu SOS thành công.", android.widget.Toast.LENGTH_SHORT).show();
                
                // Tự động load lại dữ liệu để hiển thị nhiệm vụ hiện tại và cập nhật lịch sử
                volunteerTaskViewModel.fetchCurrentMission();
                volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
            } else if (result != null && result.isError()) {
                android.widget.Toast.makeText(requireContext(), "Nhận thất bại: " + result.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void evaluateMissionRouter() {
        showRouterLoading(false);
        currentPage = 1;
        binding.tvCurrentPage.setText("Trang " + currentPage);
        volunteerTaskViewModel.fetchMissionHistoryFull(currentPage, PAGE_LIMIT);
    }

    private void routeByActionIfNeeded(int actionId) {
        if (lastRoutedId != null && lastRoutedId == actionId) {
            return;
        }

        lastRoutedId = actionId;
        navigateSafely(actionId);
    }

    private void routeByDestinationIfNeeded(int destinationId) {
        if (lastRoutedId != null && lastRoutedId == destinationId) {
            return;
        }

        lastRoutedId = destinationId;
        navigateToDestinationSafely(destinationId);
    }

    private void startGlowAnimation() {
        if (strokeAnimator != null && strokeAnimator.isRunning()) return;
        
        binding.cardSosAlert.setStrokeWidth((int) (3 * getResources().getDisplayMetrics().density));
        
        int colorStart = android.graphics.Color.parseColor("#B91C1C"); // Light Red
        int colorEnd = android.graphics.Color.parseColor("#fc81a2");   // Dark Red

        strokeAnimator = android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(), colorStart, colorEnd);
        strokeAnimator.setDuration(800);
        strokeAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        strokeAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        strokeAnimator.addUpdateListener(animator -> {
            if (binding != null && binding.cardSosAlert != null) {
                binding.cardSosAlert.setStrokeColor((int) animator.getAnimatedValue());
            }
        });
        strokeAnimator.start();
    }

    private void stopGlowAnimation() {
        if (strokeAnimator != null) {
            strokeAnimator.cancel();
            strokeAnimator = null;
        }
        if (binding != null && binding.cardSosAlert != null) {
            binding.cardSosAlert.setStrokeWidth(0);
        }
    }

    private void showRouterLoading(boolean isLoading) {
        binding.progressMissionRouter.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            binding.tvMissionPlaceholder.setVisibility(View.GONE);
            binding.rvMissionHistory.setVisibility(View.GONE);
        }
        binding.tvMissionRouterHint.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showMissionDetailPopup(com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto item) {
        android.view.View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mission_full_detail, null);
        
        android.widget.TextView tvType = dialogView.findViewById(R.id.tvDetailType);
        android.widget.TextView tvStatus = dialogView.findViewById(R.id.tvDetailStatus);
        android.widget.TextView tvAddress = dialogView.findViewById(R.id.tvDetailAddress);
        android.widget.TextView tvRadius = dialogView.findViewById(R.id.tvDetailRadius);
        android.widget.TextView tvDescription = dialogView.findViewById(R.id.tvDetailDescription);
        android.widget.TextView tvAcceptedAt = dialogView.findViewById(R.id.tvDetailAcceptedAt);
        android.widget.TextView tvPickedUpAt = dialogView.findViewById(R.id.tvDetailPickedUpAt);
        android.widget.TextView tvCompletedAt = dialogView.findViewById(R.id.tvDetailCompletedAt);
        android.widget.TextView tvCancelledAt = dialogView.findViewById(R.id.tvDetailCancelledAt);
        android.widget.TextView tvCancelReason = dialogView.findViewById(R.id.tvDetailCancelReason);
        android.widget.TextView tvPriority = dialogView.findViewById(R.id.tvDetailPriority);
        android.widget.TextView tvComment = dialogView.findViewById(R.id.tvDetailComment);
        android.widget.ImageView ivPhoto = dialogView.findViewById(R.id.ivDetailPhoto);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btnDetailClose);

        tvType.setText("Loại nhiệm vụ: " + (item.getMissionType() != null ? item.getMissionType() : "--"));
        tvStatus.setText("Trạng thái: " + (item.getStatus() != null ? item.getStatus() : "--"));
        tvAddress.setText("Địa chỉ: " + (item.getAddress() != null ? item.getAddress() : "--"));
        tvRadius.setText("Bán kính điều phối: " + (item.getRadiusKm() != null ? String.format(java.util.Locale.getDefault(), "%.2f km", item.getRadiusKm()) : "--"));
        tvDescription.setText("Mô tả: " + (item.getDescription() != null ? item.getDescription() : "--"));
        tvAcceptedAt.setText("Thời gian nhận: " + formatToVnTime(item.getAcceptedAt()));
        tvPickedUpAt.setText("Thời gian lấy hàng: " + formatToVnTime(item.getPickedUpAt()));
        tvCompletedAt.setText("Thời gian hoàn thành: " + formatToVnTime(item.getCompletedAt()));
        tvCancelledAt.setText("Thời gian hủy: " + formatToVnTime(item.getCancelledAt()));
        tvCancelReason.setText("Lý do hủy: " + (item.getCancellationReason() != null ? item.getCancellationReason() : "--"));
        tvPriority.setText("Điểm ưu tiên: " + (item.getPriorityScore() != null ? String.valueOf(item.getPriorityScore()) : "--"));
        tvComment.setText("Ghi chú: " + (item.getComment() != null ? item.getComment() : "--"));

        android.widget.TextView tvRequestHeader = dialogView.findViewById(R.id.tvRequestDetailHeader);
        android.widget.TextView tvRequestDesc = dialogView.findViewById(R.id.tvRequestDetailDesc);
        android.widget.TextView tvRequestUrgency = dialogView.findViewById(R.id.tvRequestDetailUrgency);
        android.widget.TextView tvRequestPeopleCount = dialogView.findViewById(R.id.tvRequestDetailPeopleCount);
        android.widget.TextView tvRequestItems = dialogView.findViewById(R.id.tvRequestDetailItems);
        android.view.View cardRequestBlock = dialogView.findViewById(R.id.cardRequestDetailBlock);

        if ("RESCUE".equalsIgnoreCase(item.getMissionType()) && item.getSosRequestDetail() != null) {
            cardRequestBlock.setVisibility(android.view.View.VISIBLE);
            tvRequestHeader.setText("Chi tiết Cứu hộ SOS");
            tvRequestDesc.setText("Mô tả: " + (item.getSosRequestDetail().getDescription() != null ? item.getSosRequestDetail().getDescription() : "--"));
            tvRequestUrgency.setText("Độ khẩn cấp: " + (item.getSosRequestDetail().getUrgencyLevel() != null ? item.getSosRequestDetail().getUrgencyLevel() : "--"));
            tvRequestPeopleCount.setText("Số lượng người: " + (item.getSosRequestDetail().getPeopleCount() != null ? item.getSosRequestDetail().getPeopleCount() : "--"));
            tvRequestItems.setVisibility(android.view.View.GONE);
        } else if (item.getAidRequestDetail() != null) {
            cardRequestBlock.setVisibility(android.view.View.VISIBLE);
            tvRequestHeader.setText("Chi tiết Cứu trợ Vật phẩm");
            tvRequestDesc.setText("Mô tả: " + (item.getAidRequestDetail().getDescription() != null ? item.getAidRequestDetail().getDescription() : "--"));
            tvRequestUrgency.setText("Trạng thái yêu cầu: " + (item.getAidRequestDetail().getStatus() != null ? item.getAidRequestDetail().getStatus() : "--"));
            tvRequestPeopleCount.setText(String.format(java.util.Locale.getDefault(), "Người lớn: %d, Người già: %d, Trẻ em: %d", 
                item.getAidRequestDetail().getNumberAdult() != null ? item.getAidRequestDetail().getNumberAdult() : 0,
                item.getAidRequestDetail().getNumberElderly() != null ? item.getAidRequestDetail().getNumberElderly() : 0,
                item.getAidRequestDetail().getNumberChildren() != null ? item.getAidRequestDetail().getNumberChildren() : 0));
            tvRequestItems.setVisibility(android.view.View.GONE);
        } else {
            cardRequestBlock.setVisibility(android.view.View.GONE);
        }

        String photoUrl = item.getImageUrl() != null ? item.getImageUrl() : item.getConfirmationImageUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            ivPhoto.setVisibility(android.view.View.VISIBLE);
            com.bumptech.glide.Glide.with(this).load(photoUrl).into(ivPhoto);
        } else {
            ivPhoto.setVisibility(android.view.View.GONE);
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private String formatToVnTime(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) return "--";
        try {
            java.time.Instant instant = java.time.Instant.parse(isoString);
            java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return isoString;
        }
    }
}
