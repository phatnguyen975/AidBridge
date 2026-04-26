package com.drc.aidbridge.ui.main.fragment.sponsor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SponsorQrCodeFragment extends BaseFragment<FragmentSponsorQrCodeBinding> {

    public static final String ARG_DONATION_CODE = "arg_donation_code";
    public static final String ARG_QR_CODE_TOKEN = "arg_qr_code_token";
    public static final String ARG_ITEM_NAME = "arg_item_name";
    public static final String ARG_QUANTITY_TEXT = "arg_quantity_text";
    private static final String QR_IMAGE_MIME_TYPE = "image/png";
    private static final String QR_IMAGE_FOLDER = "Pictures/AidBridge";
    private static final String QR_IMAGE_FILE_PREFIX = "aidbridge_qr_";

    private Bitmap generatedQrBitmap;

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

        binding.btnSaveQr.setOnClickListener(v -> {
            if (generatedQrBitmap == null) {
                showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_generate_failed), true);
                return;
            }

            boolean isSaved = saveQrImageToGallery(generatedQrBitmap, donationCode);
            if (isSaved) {
                showToast(getString(R.string.sponsor_qr_save_success));
            } else {
                showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_save_failed), true);
            }
        });
    }

    @Override
    protected void observeViewModel() {
        // TODO: Add Sponsor QR ViewModel observers when real data is available.
    }

    private void renderQrCode(String qrToken, int sizePx) {
        if (qrToken.isEmpty()) {
            generatedQrBitmap = null;
            binding.ivQrCode.setImageResource(R.mipmap.ic_launcher);
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_missing_token), true);
            return;
        }

        try {
            BitMatrix matrix = new QRCodeWriter().encode(qrToken, BarcodeFormat.QR_CODE, sizePx, sizePx);
            generatedQrBitmap = toBitmap(matrix);
            binding.ivQrCode.setImageBitmap(generatedQrBitmap);
        } catch (WriterException exception) {
            generatedQrBitmap = null;
            binding.ivQrCode.setImageResource(R.mipmap.ic_launcher);
            showTopSnackbar(binding.getRoot(), getString(R.string.sponsor_qr_generate_failed), true);
        }
    }

    private boolean saveQrImageToGallery(Bitmap bitmap, String donationCode) {
        if (getContext() == null) {
            return false;
        }

        ContentResolver contentResolver = requireContext().getContentResolver();
        String fileName = buildQrFileName(donationCode);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, QR_IMAGE_MIME_TYPE);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, QR_IMAGE_FOLDER);

        Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri == null) {
            return false;
        }

        try (OutputStream outputStream = contentResolver.openOutputStream(imageUri)) {
            if (outputStream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                contentResolver.delete(imageUri, null, null);
                return false;
            }
            outputStream.flush();
            return true;
        } catch (IOException | SecurityException exception) {
            contentResolver.delete(imageUri, null, null);
            return false;
        }
    }

    private String buildQrFileName(String donationCode) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String normalizedDonationCode = safeText(donationCode).replaceAll("[^a-zA-Z0-9_-]", "");
        if (normalizedDonationCode.isEmpty()) {
            return QR_IMAGE_FILE_PREFIX + timestamp + ".png";
        }
        return QR_IMAGE_FILE_PREFIX + normalizedDonationCode + "_" + timestamp + ".png";
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
