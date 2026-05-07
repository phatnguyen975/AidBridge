package com.drc.aidbridge.ui.map.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.ui.map.base.BaseMapFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimMapViewModel;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimHistoryViewModel;
import com.drc.aidbridge.ui.map.realtime.VictimLocationListener;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import androidx.core.content.ContextCompat;
import com.drc.aidbridge.R;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter.HistoryModel;
import dagger.hilt.android.AndroidEntryPoint;
import com.drc.aidbridge.BuildConfig;
@AndroidEntryPoint
public class VictimMapFragment extends BaseMapFragment<VictimMapViewModel> {

    private VictimMapViewModel victimMapViewModel;
    private VictimHistoryViewModel victimHistoryViewModel;
    private VictimLocationListener locationListener;
    private Marker volunteerMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        victimMapViewModel = new ViewModelProvider(this).get(VictimMapViewModel.class);
        victimHistoryViewModel = new ViewModelProvider(requireActivity()).get(VictimHistoryViewModel.class);
    }

    @Override
    protected VictimMapViewModel getViewModel() {
        if (victimMapViewModel == null) {
            victimMapViewModel = new ViewModelProvider(this).get(VictimMapViewModel.class);
        }
        return victimMapViewModel;
    }

    @Override
    protected int getContentLayout() {
        return R.layout.fragment_map_victim;
    }

    @Override
    protected void setupRoleSpecificUI() {
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        // Trigger loading history to find active missions
        victimHistoryViewModel.loadInitial(true, "STATUS_PROCESSING");
    }

    @Override
    protected void observeViewModel() {
        super.observeViewModel();
        victimHistoryViewModel.getHistoryResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                // Find the first PROCESSING mission
                for (HistoryModel item : result.getData().getItems()) {
                    if (HistoryModel.STATUS_PROCESSING.equalsIgnoreCase(item.statusType)) {
                        Log.i("VictimMap", "Found active mission: " + item.id);
                        initLocationListener(item.id);
                        break;
                    }
                }
            }
        });
    }

    private void initLocationListener(String missionId) {
        if (locationListener != null) return;

        Log.d("VictimMap", "Initializing tracking listener for missionId: " + missionId);
        locationListener = new VictimLocationListener(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_ANON_KEY,
                missionId
        );

        locationListener.connect(
            (latitude, longitude, heading) -> {
                GeoPoint volunteerPoint = new GeoPoint(latitude, longitude);
                updateVolunteerMarker(volunteerPoint);
            },
            points -> {
                if (points == null || points.isEmpty()) return;
                
                // Clear existing route overlays first
                clearRouteOverlays();
                
                // Draw the planned route in YELLOW, shouldZoom = FALSE
                drawRouteOverlays(
                    points,
                    R.color.route_yellow_casing,
                    R.dimen.volunteer_map_route_casing_stroke_width,
                    R.color.route_yellow,
                    R.dimen.volunteer_map_route_stroke_width,
                    R.dimen.spacing_xl,
                    false
                );
                
                if (mapView != null) {
                    mapView.invalidate();
                }
            }
        );
    }

    private void updateVolunteerMarker(GeoPoint point) {
        if (mapView == null) return;

        if (volunteerMarker == null) {
            volunteerMarker = new Marker(mapView);
            volunteerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            volunteerMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_volunteer_location_circle));
            volunteerMarker.setTitle("Vị trí Tình nguyện viên");
            mapView.getOverlays().add(volunteerMarker);
        }
        
        volunteerMarker.setPosition(point);
        mapView.invalidate();
    }

    @Override
    public void onDestroyView() {
        if (locationListener != null) {
            locationListener.disconnect();
            locationListener = null;
        }
        super.onDestroyView();
    }
}
