package com.drc.aidbridge.ui.map.fragment;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;
import com.drc.aidbridge.databinding.FragmentMapAdminBinding;
import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminMapViewModel;
import com.drc.aidbridge.ui.map.base.helper.MapMarkerHelper;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminMapFragment extends BaseMapFragment<AdminMapViewModel> {

    private AdminMapViewModel adminMapViewModel;
    private FragmentMapAdminBinding adminBinding;
    private boolean isMenuOpen = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View root = view.findViewById(R.id.adminMapRoot);
        adminBinding = FragmentMapAdminBinding.bind(root);
        
        // Force Admin content to be on top of BaseMap elements (like search button)
        root.setTranslationZ(100f);
        if (root.getParent() instanceof View) {
            ((View) root.getParent()).setTranslationZ(100f);
        }
        
        setupAdminUI();
    }

    @Override
    protected AdminMapViewModel getViewModel() {
        if (adminMapViewModel == null) {
            adminMapViewModel = new ViewModelProvider(this).get(AdminMapViewModel.class);
        }
        return adminMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return R.layout.fragment_map_admin;
    }

    @Override
    protected void setupRoleSpecificUI() {
        // Handled in setupAdminUI() called from onViewCreated
    }

    private void setupAdminUI() {
        // Toggle Menu
        adminBinding.btnToggleRoutingMenu.setOnClickListener(v -> toggleMenu());

        // Status Dropdown (Material 3)
        String[] statuses = {"PENDING", "ACCEPTED", "COMPLETED", "CANCELED", "ARRIVED", "DISPATCHED"};
        String[] displayStatuses = {"Đang chờ", "Đã chấp nhận", "Đã hoàn thành", "Đã hủy", "Đã đến nơi", "Đã điều phối"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, displayStatuses);
        adminBinding.autoCompleteStatus.setAdapter(adapter);

        // Apply Filter
        adminBinding.btnApplyFilter.setOnClickListener(v -> {
            String selectedText = adminBinding.autoCompleteStatus.getText().toString();
            String status = statuses[0]; // Default
            for (int i = 0; i < displayStatuses.length; i++) {
                if (displayStatuses[i].equals(selectedText)) {
                    status = statuses[i];
                    break;
                }
            }
            
            String start = adminBinding.etStartDate.getText().toString();
            String end = adminBinding.etEndDate.getText().toString();
            getViewModel().fetchSosAidRequests(status, start.isEmpty() ? null : start, end.isEmpty() ? null : end);
            toggleMenu(); 
        });

        // Date Pickers
        adminBinding.etStartDate.setOnClickListener(v -> showDatePicker(true));
        adminBinding.etEndDate.setOnClickListener(v -> showDatePicker(false));

        // Clear Markers
        adminBinding.btnClearMarkers.setOnClickListener(v -> {
            mapMarkerHelper.clearAdminMarkers();
            Toast.makeText(requireContext(), "Đã xóa các đánh dấu", Toast.LENGTH_SHORT).show();
        });

        // Observe Data
        getViewModel().getSosAidRequests().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                drawAdminData(result.getData());
            } else if (result.isError()) {
                Toast.makeText(requireContext(), "Lỗi: " + result.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleMenu() {
        isMenuOpen = !isMenuOpen;
        float translationX = isMenuOpen ? 330f * getResources().getDisplayMetrics().density : 0f;
        adminBinding.cardRoutingMenu.animate().translationX(translationX).setDuration(300).start();
        adminBinding.btnToggleRoutingMenu.animate().translationX(translationX).setDuration(300).start();
        adminBinding.btnToggleRoutingMenu.setImageResource(isMenuOpen ? R.drawable.ic_admin_map_chevron_left : R.drawable.ic_admin_map_chevron_right);
        
        // Hide/Show BaseMap search FAB to avoid overlap
        if (binding != null && binding.fabFindHubs != null) {
            binding.fabFindHubs.setVisibility(isMenuOpen ? View.GONE : View.VISIBLE);
        }
    }

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? "Chọn ngày bắt đầu" : "Chọn ngày kết thúc")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            String dateStr = dateFormat.format(new Date(selection));
            if (isStart) {
                adminBinding.etStartDate.setText(dateStr);
            } else {
                adminBinding.etEndDate.setText(dateStr);
            }
        });
        picker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    private void drawAdminData(AdminRoutingSosAidResponseDto data) {
        // Create custom pulse drawables for SOS (Red) and Aid (Yellow)
        PulseDrawable sosPulse = new PulseDrawable(Color.RED);
        PulseDrawable aidPulse = new PulseDrawable(Color.parseColor("#FFD600")); // Vibrant yellow

        mapMarkerHelper.drawAdminMarkers(requireContext(), data.getSosRequests(), data.getAidRequests(), sosPulse, aidPulse, new MapMarkerHelper.OnAdminMarkerClickListener() {
            @Override
            public void onSosClicked(AdminRoutingSosAidResponseDto.AdminSosRequestDto sos) {
                showDetailDialog("Yêu cầu SOS", sos.getLat(), sos.getLng(), sos.getCreatedAt(), sos.getAddress(), sos.getDescription());
            }

            @Override
            public void onAidClicked(AdminRoutingSosAidResponseDto.AdminAidRequestDto aid) {
                showDetailDialog("Yêu cầu hỗ trợ", aid.getLat(), aid.getLng(), aid.getCreatedAt(), aid.getAddress(), aid.getDescription());
            }
        });

        // Start animation by invalidating the map periodically
        startMarkerAnimation();
    }

    private Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mapView != null) {
                mapView.invalidate();
                mainHandler.postDelayed(this, 33); // ~30 FPS
            }
        }
    };

    private void startMarkerAnimation() {
        mainHandler.removeCallbacks(animationRunnable);
        mainHandler.post(animationRunnable);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(animationRunnable);
        super.onDestroyView();
    }

    private void showDetailDialog(String title, Double lat, Double lng, String createdAt, String address, String description) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(String.format(Locale.getDefault(),
                        "Tọa độ: %.6f, %.6f\nThời gian: %s\nĐịa chỉ: %s\nMô tả: %s",
                        lat, lng, createdAt, address, description))
                .setPositiveButton("Đóng", null)
                .show();
    }

    /**
     * Custom Drawable that pulses (scales up and down) and has a clear border
     */
    private static class PulseDrawable extends GradientDrawable {
        private final Paint borderPaint;
        private final Paint fillPaint;
        private final int baseSize = 50; // Larger base size

        public PulseDrawable(int color) {
            setShape(OVAL);
            setSize(baseSize * 2, baseSize * 2);
            
            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);
            
            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(8f);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();

            // Calculate pulse scale using sine wave
            long time = System.currentTimeMillis();
            float scale = 0.8f + 0.3f * (float) Math.abs(Math.sin(time / 400.0));

            float radius = (baseSize * scale);

            // Draw shadow
            Paint shadowPaint = new Paint(fillPaint);
            shadowPaint.setShadowLayer(12f, 0, 4f, Color.parseColor("#40000000"));
            canvas.drawCircle(centerX, centerY, radius, shadowPaint);

            // Draw border
            canvas.drawCircle(centerX, centerY, radius + 4, borderPaint);
            
            // Draw fill
            canvas.drawCircle(centerX, centerY, radius, fillPaint);
        }
    }
}
