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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.request.DangerousZoneRequestDto;
import com.drc.aidbridge.data.remote.dto.response.DangerousZoneResponseDto;
import com.drc.aidbridge.data.remote.dto.response.GeoJsonGeometryDto;
import com.drc.aidbridge.data.remote.dto.response.admin.AdminRoutingSosAidResponseDto;
import com.drc.aidbridge.databinding.FragmentMapAdminBinding;
import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminMapViewModel;
import com.drc.aidbridge.ui.map.base.helper.DangerousZoneHelper;
import com.drc.aidbridge.ui.map.base.helper.MapMarkerHelper;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polygon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminMapFragment extends BaseMapFragment<AdminMapViewModel> {

    private AdminMapViewModel adminMapViewModel;
    private FragmentMapAdminBinding adminBinding;
    private DangerousZoneHelper dangerousZoneHelper;
    private boolean isMenuOpen = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View root = view.findViewById(R.id.adminMapRoot);
        adminBinding = FragmentMapAdminBinding.bind(root);
        
        // Force Admin panels to be on top of BaseMap elements (like search button)
        adminBinding.cardRoutingMenu.setTranslationZ(150f);
        adminBinding.btnToggleRoutingMenu.setTranslationZ(150f);
        adminBinding.cardDrawingToolbar.setTranslationZ(150f);
        adminBinding.cardDrawingName.setTranslationZ(150f);
        adminBinding.ivDrawingCrosshair.setTranslationZ(150f);
        
        setupAdminUI();
        
        // Ensure navigation HUD and Search Hub are always hidden for Admins
        if (binding != null) {
            if (binding.cardNavigationHud != null) {
                binding.cardNavigationHud.setVisibility(View.GONE);
            }
            if (binding.fabFindHubs != null) {
                binding.fabFindHubs.setVisibility(View.GONE);
            }
            if (binding.drawerLayout != null) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
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
        String[] statuses = {"PENDING", "ASSIGNED", "COMPLETED", "CANCELED"};
        String[] displayStatuses = {"Đang chờ", "Đã phân công", "Đã hoàn thành", "Đã hủy"};
        
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
        adminBinding.btnClearAllMarkers.setOnClickListener(v -> {
            mapMarkerHelper.clearAdminMarkers();
            Toast.makeText(requireContext(), "Đã xóa các đánh dấu", Toast.LENGTH_SHORT).show();
        });
        
        // Toggle Zones Visibility
        adminBinding.btnClearAllZones.setOnClickListener(v -> {
            if (dangerousZoneHelper != null) {
                dangerousZoneHelper.toggleZonesVisibility();
            }
        });

        // Observe SOS/Aid requests
        getViewModel().getSosAidRequests().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                drawAdminData(result.getData());
            } else if (result.isError()) {
                Toast.makeText(requireContext(), "Lỗi SOS: " + result.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Observe Dangerous Zones list from Server
        getViewModel().getServerDangerousZones().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                if (dangerousZoneHelper != null) {
                    dangerousZoneHelper.clearAllPermanentPolygons();
                    for (DangerousZoneResponseDto zone : result.getData()) {
                        Polygon poly = new Polygon();
                        poly.setId(zone.getId().toString());
                        poly.setTitle(zone.getName());
                        
                        List<GeoPoint> points = new ArrayList<>();
                        for (List<Double> coord : zone.getGeometry().getCoordinates().get(0)) {
                            points.add(new GeoPoint(coord.get(1), coord.get(0)));
                        }
                        poly.setPoints(points);
                        poly.setFillColor(Color.argb(50, 211, 47, 47));
                        poly.setStrokeColor(Color.RED);
                        poly.setStrokeWidth(2f);
                        dangerousZoneHelper.addPermanentPolygon(poly);
                    }
                }
            }
        });

        // Observe Mutation results
        getViewModel().getZoneOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(requireContext(), "Đã lưu vùng nguy hiểm", Toast.LENGTH_SHORT).show();
                    adminBinding.etDrawingName.setText(""); // Clear input
                } else if (result.isError()) {
                    Toast.makeText(requireContext(), "Lỗi lưu vùng: " + result.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        getViewModel().getZoneDeleteResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    Toast.makeText(requireContext(), "Đã xóa vùng nguy hiểm", Toast.LENGTH_SHORT).show();
                } else if (result.isError()) {
                    Toast.makeText(requireContext(), "Lỗi xóa vùng: " + result.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // Dangerous Zone Helper
        dangerousZoneHelper = new DangerousZoneHelper(
                mapView, adminBinding, binding, new DangerousZoneHelper.OnDangerousZoneListener() {
            @Override
            public void onZoneSaved(@Nullable UUID id, String name, List<GeoPoint> points) {
                // Prepare request
                List<List<Double>> ring = new ArrayList<>();
                for (GeoPoint p : points) {
                    ring.add(Arrays.asList(p.getLongitude(), p.getLatitude()));
                }
                // Close the loop
                ring.add(Arrays.asList(points.get(0).getLongitude(), points.get(0).getLatitude()));
                
                GeoJsonGeometryDto geometry = 
                        new GeoJsonGeometryDto("Polygon", Collections.singletonList(ring));
                
                DangerousZoneRequestDto request = 
                        new DangerousZoneRequestDto(name, geometry, getViewModel().getCurrentAdminId());
                
                getViewModel().saveDangerousZone(id, request);
            }

            @Override
            public void onDeleteZone(UUID id) {
                getViewModel().deleteDangerousZone(id);
            }

            @Override
            public void onEditModeChanged(boolean isEditing) {
                if (!isEditing && binding != null && binding.fabFindHubs != null) {
                    binding.fabFindHubs.setVisibility(View.GONE);
                }
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
