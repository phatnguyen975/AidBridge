package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimRescueTabBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.adapter.victim.VictimImageAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimRescueTabFragment extends BaseFragment<FragmentVictimRescueTabBinding> {

    private VictimImageAdapter imageAdapter;

    @Nullable
    @Override
    protected FragmentVictimRescueTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimRescueTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupImageRecyclerView();
        setupSeverityDefault();
        setupInteractions();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Implement ViewModel observation for rescue data when backend integration is ready.
    }

    private void setupImageRecyclerView() {
        imageAdapter = new VictimImageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvImages.setLayoutManager(layoutManager);
        binding.rvImages.setAdapter(imageAdapter);
    }

    private void setupSeverityDefault() {
        String[] levels = getResources().getStringArray(R.array.sos_severity_levels);
        if (levels.length > 0) {
            binding.actSeverity.setText(levels[0], false);
        }
    }

    private void setupInteractions() {
        binding.btnSos.setOnClickListener(v -> extractDataAndSubmit());

        binding.cvUploadArea.setOnClickListener(v -> {
            // TODO: Implement image picker and handle permissions when ready. Use ActivityResultContracts for better lifecycle handling.
            imageAdapter.addImage(R.drawable.ic_rescue);
            imageAdapter.addImage(R.drawable.ic_relative_support);
        });
    }

    private void extractDataAndSubmit() {
        RescueFormInput rawInput = collectRawInput();
        // TODO: Call ViewModel method to submit the rescue request with the collected input. Handle loading state and API response.
        Toast.makeText(requireContext(), "Đang gửi API...", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private RescueFormInput collectRawInput() {
        String fullName = String.valueOf(binding.etFullName.getText()).trim();
        String peopleCount = String.valueOf(binding.etPeopleCount.getText()).trim();
        String severity = String.valueOf(binding.actSeverity.getText()).trim();
        String healthDetail = String.valueOf(binding.etHealthDetail.getText()).trim();

        return new RescueFormInput(fullName, peopleCount, severity, healthDetail);
    }

    private static final class RescueFormInput {
        final String fullName;
        final String peopleCount;
        final String severity;
        final String healthDetail;

        RescueFormInput(String fullName, String peopleCount, String severity, String healthDetail) {
            this.fullName = fullName;
            this.peopleCount = peopleCount;
            this.severity = severity;
            this.healthDetail = healthDetail;
        }
    }
}
