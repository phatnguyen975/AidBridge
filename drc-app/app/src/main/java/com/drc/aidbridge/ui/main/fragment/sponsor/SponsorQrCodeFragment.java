package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorQrCodeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorQrCodeFragment extends BaseFragment<FragmentSponsorQrCodeBinding> {

    @Nullable
    @Override
    protected FragmentSponsorQrCodeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorQrCodeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivClose.setOnClickListener(v -> popBackStackSafely());

        binding.ivQrCode.setImageResource(R.mipmap.ic_launcher);
        int desiredQrSize = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_image_size);
        int qrCardSize = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_card_size);
        int qrCardPadding = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_card_inner_padding);
        int maxQrSize = Math.max(0, qrCardSize - (qrCardPadding * 2));
        int finalQrSize = Math.min(desiredQrSize, maxQrSize);

        FrameLayout.LayoutParams qrLayoutParams = new FrameLayout.LayoutParams(finalQrSize, finalQrSize);
        qrLayoutParams.gravity = Gravity.CENTER;
        binding.ivQrCode.setLayoutParams(qrLayoutParams);
        binding.ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

        binding.tvDonationCodeValue.setText(getString(R.string.sponsor_qr_mock_donation_code));
        binding.tvItemTypeValue.setText(getString(R.string.sponsor_qr_mock_item_type));
        binding.tvQuantityValue.setText(getString(R.string.sponsor_qr_mock_quantity));

        binding.btnSaveQr.setOnClickListener(v -> showToast("Đã lưu mã QR"));
    }

    @Override
    protected void observeViewModel() {
        // TODO: Add Sponsor QR ViewModel observers when real data is available.
    }
}
