package com.drc.aidbridge.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Utility methods for image processing before upload.
 */
public final class ImageUtils {

    private static final int AVATAR_MAX_WIDTH = 800;
    private static final int AVATAR_MAX_HEIGHT = 800;
    private static final int AVATAR_QUALITY = 70;

    private static final int SCENE_MAX_WIDTH = 1280;
    private static final int SCENE_MAX_HEIGHT = 1280;
    private static final int SCENE_QUALITY = 85;

    private ImageUtils() {
    }

    public static File compressAvatar(@NonNull Context context, @NonNull Uri imageUri) throws IOException {
        return compressImage(context, imageUri, AVATAR_MAX_WIDTH, AVATAR_MAX_HEIGHT, AVATAR_QUALITY);
    }

    public static File compressScenePhoto(@NonNull Context context, @NonNull Uri imageUri) throws IOException {
        return compressImage(context, imageUri, SCENE_MAX_WIDTH, SCENE_MAX_HEIGHT, SCENE_QUALITY);
    }

    public static String createScenePhotoDataUrl(@NonNull Context context, @NonNull Uri imageUri) throws IOException {
        File compressedFile = compressScenePhoto(context, imageUri);
        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(compressedFile.toPath());
            String mimeType = resolveMimeType(compressedFile.getName());
            String base64Payload = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            return "data:" + mimeType + ";base64," + base64Payload;
        } finally {
            // Best effort cleanup for temporary compressed file.
            compressedFile.delete();
        }
    }

    public static MultipartBody.Part createAvatarMultipart(@NonNull File compressedFile) {
        String mimeType = resolveMimeType(compressedFile.getName());
        MediaType mediaType = MediaType.get(mimeType);
        RequestBody requestBody = RequestBody.create(compressedFile, mediaType);
        return MultipartBody.Part.createFormData("avatar", compressedFile.getName(), requestBody);
    }

    public static File compressImage(@NonNull Context context,
                                     @NonNull Uri imageUri,
                                     int maxWidth,
                                     int maxHeight,
                                     int quality) throws IOException {
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("maxWidth and maxHeight must be > 0");
        }

        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("quality must be in range 1..100");
        }

        Bitmap bitmap = decodeBitmap(context, imageUri, maxWidth, maxHeight);
        if (bitmap == null) {
            throw new IOException("Unable to decode selected image");
        }

        Bitmap scaledBitmap = scaleDownKeepingRatio(bitmap, maxWidth, maxHeight);
        try {
            String mimeType = context.getContentResolver().getType(imageUri);
            boolean pngWithAlpha = isPng(mimeType) && bitmap.hasAlpha();
            Bitmap.CompressFormat format = resolveCompressFormat(pngWithAlpha);

            byte[] compressedBytes = compressBitmap(scaledBitmap, format, quality, pngWithAlpha);

            String extension = resolveExtension(format, pngWithAlpha);
            File outputFile = new File(
                context.getCacheDir(),
                "image_compressed_" + System.currentTimeMillis() + "." + extension
            );

            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                outputStream.write(compressedBytes);
                outputStream.flush();
            }

            return outputFile;
        } finally {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (scaledBitmap != bitmap && !scaledBitmap.isRecycled()) {
                scaledBitmap.recycle();
            }
        }
    }

    private static Bitmap decodeBitmap(@NonNull Context context,
                                       @NonNull Uri imageUri,
                                       int maxWidth,
                                       int maxHeight) throws IOException {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;

        try (InputStream boundsStream = context.getContentResolver().openInputStream(imageUri)) {
            if (boundsStream == null) {
                throw new IOException("Unable to open image stream");
            }
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight);
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try (InputStream decodeStream = context.getContentResolver().openInputStream(imageUri)) {
            if (decodeStream == null) {
                throw new IOException("Unable to open image stream");
            }
            return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        }
    }

    private static Bitmap scaleDownKeepingRatio(@NonNull Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float widthRatio = (float) maxWidth / (float) width;
        float heightRatio = (float) maxHeight / (float) height;
        float ratio = Math.min(widthRatio, heightRatio);

        int targetWidth = Math.max(1, Math.round(width * ratio));
        int targetHeight = Math.max(1, Math.round(height * ratio));
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private static byte[] compressBitmap(@NonNull Bitmap bitmap,
                                         @NonNull Bitmap.CompressFormat format,
                                         int quality,
                                         boolean preserveAlpha) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int effectiveQuality = preserveAlpha ? 100 : quality;
        bitmap.compress(format, effectiveQuality, outputStream);
        return outputStream.toByteArray();
    }

    private static Bitmap.CompressFormat resolveCompressFormat(boolean pngWithAlpha) {
        if (!pngWithAlpha) {
            return Bitmap.CompressFormat.JPEG;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Bitmap.CompressFormat.WEBP_LOSSLESS;
        }

        return Bitmap.CompressFormat.PNG;
    }

    private static boolean isPng(String mimeType) {
        return mimeType != null && mimeType.equalsIgnoreCase("image/png");
    }

    private static String resolveExtension(Bitmap.CompressFormat format, boolean preserveAlpha) {
        if (preserveAlpha) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && format == Bitmap.CompressFormat.WEBP_LOSSLESS) {
                return "webp";
            }
            return "png";
        }
        return "jpg";
    }

    private static String resolveMimeType(String fileName) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return Math.max(inSampleSize, 1);
    }
}
