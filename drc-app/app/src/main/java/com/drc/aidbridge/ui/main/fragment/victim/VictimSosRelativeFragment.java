package com.drc.aidbridge.ui.main.fragment.victim;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimSosRelativeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSosRelativeFragment extends BaseFragment<FragmentVictimSosRelativeBinding> {

    @Nullable
    @Override
    protected FragmentVictimSosRelativeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSosRelativeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        String[] severityLevels = getResources().getStringArray(R.array.sos_severity_levels);
        if (severityLevels.length > 0) {
            binding.actRelativeSeverity.setText(severityLevels[0], false);
        }

        binding.btnSubmitRelativeSos.setOnClickListener(v -> extractDataAndSubmit());
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe relative SOS flow when ViewModel integration is implemented.
    }

    private void extractDataAndSubmit() {
        RelativeSosFormInput rawInput = collectRawInput();
        // TODO: Pass to ViewModel for validation and API call.
        Toast.makeText(requireContext(), "Đang gửi yêu cầu...", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private RelativeSosFormInput collectRawInput() {
        String name = String.valueOf(binding.etRelativeName.getText()).trim();
        String address = String.valueOf(binding.etRelativeAddress.getText()).trim();
        String phone = String.valueOf(binding.etRelativePhone.getText()).trim();
        String severity = String.valueOf(binding.actRelativeSeverity.getText()).trim();

        return new RelativeSosFormInput(name, address, phone, severity);
    }

    private static final class RelativeSosFormInput {
        final String name;
        final String address;
        final String phone;
        final String severity;

        RelativeSosFormInput(String name, String address, String phone, String severity) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.severity = severity;
        }
    }
}
