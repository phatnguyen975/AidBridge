package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentSponsorQrCodeBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorQrCodeFragment extends BaseFragment<FragmentSponsorQrCodeBinding> {

    public static final String ARG_DONATION_CODE = "arg_donation_code";
    public static final String ARG_QR_CODE_TOKEN = "arg_qr_code_token";
    public static final String ARG_ITEM_NAME = "arg_item_name";
    public static final String ARG_QUANTITY_TEXT = "arg_quantity_text";

    @Nullable
    @Override
    protected FragmentSponsorQrCodeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSponsorQrCodeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.ivClose.setOnClickListener(v -> popBackStackSafely());

        int desiredQrSize = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_image_size);
        int qrCardSize = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_card_size);
        int qrCardPadding = getResources().getDimensionPixelSize(R.dimen.sponsor_qr_card_inner_padding);
        int maxQrSize = Math.max(0, qrCardSize - (qrCardPadding * 2));
        int finalQrSize = Math.min(desiredQrSize, maxQrSize);

        FrameLayout.LayoutParams qrLayoutParams = new FrameLayout.LayoutParams(finalQrSize, finalQrSize);
        qrLayoutParams.gravity = Gravity.CENTER;
        binding.ivQrCode.setLayoutParams(qrLayoutParams);
        binding.ivQrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Bundle args = getArguments();
        String donationCode = safeText(args != null ? args.getString(ARG_DONATION_CODE) : null);
        String qrToken = safeText(args != null ? args.getString(ARG_QR_CODE_TOKEN) : null);
        String itemName = safeText(args != null ? args.getString(ARG_ITEM_NAME) : null);
        String quantityText = safeText(args != null ? args.getString(ARG_QUANTITY_TEXT) : null);

        binding.tvDonationCodeValue.setText(!donationCode.isEmpty() ? donationCode : getString(R.string.sponsor_qr_missing_value));
        binding.tvItemTypeValue.setText(!itemName.isEmpty() ? itemName : getString(R.string.sponsor_qr_missing_value));
        binding.tvQuantityValue.setText(!quantityText.isEmpty() ? quantityText : getString(R.string.sponsor_qr_missing_value));

        renderQrCode(qrToken, finalQrSize);

        binding.btnSaveQr.setOnClickListener(v -> showToast(getString(R.string.sponsor_qr_save_success)));
    }

    @Override
    protected void observeViewModel() {
        // TODO: Add Sponsor QR ViewModel observers when real data is available.
    }

    private void renderQrCode(String qrToken, int sizePx) {
        if (qrToken.isEmpty()) {
            binding.ivQrCode.setImageResource(R.mipmap.ic_launcher);
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_missing_token), true);
            return;
        }

        try {
            BitMatrix matrix = new QRCodeWriter().encode(qrToken, BarcodeFormat.QR_CODE, sizePx, sizePx);
            binding.ivQrCode.setImageBitmap(toBitmap(matrix));
        } catch (WriterException exception) {
            binding.ivQrCode.setImageResource(R.mipmap.ic_launcher);
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_generate_failed), true);
        }
    }

    private Bitmap toBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        return bitmap;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
