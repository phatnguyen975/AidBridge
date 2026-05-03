package com.drc.aidbridge.ui.map.feature.hub;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.hub.HubDto;
import com.drc.aidbridge.ui.main.viewmodel.volunteer.VolunteerMapViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import org.osmdroid.util.GeoPoint;
import com.drc.aidbridge.ui.map.base.BaseMapViewModel;

import java.util.Locale;

public class HubSearchDrawerFragment extends Fragment {

    private BaseMapViewModel viewModel;
    private HubSearchAdapter adapter;
    private HubSearchListener listener;

    public void setViewModel(BaseMapViewModel viewModel) {
        this.viewModel = viewModel;
        if (isAdded() && getView() != null && viewModel != null) {
            observeViewModelResults();
        }
    }

    private RadioGroup rgLocationSource;
    private RadioButton rbCurrentLocation;
    private RadioButton rbStartPoint;
    private Slider sliderRadius;
    private TextView tvRadiusLabel;
    private MaterialButton btnSearchHubs;
    private RecyclerView rvHubs;
    private ProgressBar progressHubSearch;
    private TextView tvEmptyState;

    public interface HubSearchListener {
        void onHubSelected(HubDto hub);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hub_search_drawer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (viewModel != null) {
            observeViewModelResults();
        }

        rgLocationSource = view.findViewById(R.id.rgLocationSource);
        rbCurrentLocation = view.findViewById(R.id.rbCurrentLocation);
        rbStartPoint = view.findViewById(R.id.rbStartPoint);
        sliderRadius = view.findViewById(R.id.sliderRadius);
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel);
        btnSearchHubs = view.findViewById(R.id.btnSearchHubs);
        rvHubs = view.findViewById(R.id.rvHubs);
        progressHubSearch = view.findViewById(R.id.progressHubSearch);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        adapter = new HubSearchAdapter(hub -> {
            if (listener != null) {
                listener.onHubSelected(hub);
            }
        });

        rvHubs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHubs.setAdapter(adapter);

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            tvRadiusLabel.setText(String.format(Locale.getDefault(), "Bán kính tìm kiếm: %.1f km", value));
        });

        btnSearchHubs.setOnClickListener(v -> performSearch());
    }

    private void observeViewModelResults() {
        if (viewModel == null)
            return;
        viewModel.getHubSearchResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isLoading()) {
                progressHubSearch.setVisibility(View.VISIBLE);
                rvHubs.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                progressHubSearch.setVisibility(View.GONE);
                if (result.getData() != null && !result.getData().isEmpty()) {
                    adapter.setHubs(result.getData());
                    rvHubs.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                } else {
                    adapter.setHubs(null);
                    rvHubs.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Không tìm thấy Hub nào trong bán kính.");
                }
            } else if (result.isError()) {
                progressHubSearch.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Lỗi kết nối.");
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch() {
        GeoPoint location = null;
        if (rbCurrentLocation.isChecked()) {
            location = viewModel.getCurrentPoint();
        } else if (rbStartPoint.isChecked()) {
            location = viewModel.getStartPoint();
        }

        if (location == null) {
            Toast.makeText(requireContext(), "Chưa xác định được vị trí của bạn.", Toast.LENGTH_LONG).show();
            return;
        }

        double radiusKm = sliderRadius.getValue();
        viewModel.searchHubsNearLocation("ACTIVE", location.getLatitude(), location.getLongitude(), radiusKm);
    }

    public void setListener(HubSearchListener listener) {
        this.listener = listener;
    }
}
