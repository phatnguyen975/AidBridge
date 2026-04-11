package com.drc.aidbridge.modules.sos.internal.service;

import com.drc.aidbridge.modules.attachment.internal.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves SOS scene image input into a stable URL stored in database.
 * Supports either direct public URL or base64 data URL and uploads base64 payload to Cloudinary.
 */
@Component
@RequiredArgsConstructor
public class SosSceneImageService {

    private static final Pattern DATA_IMAGE_URL_PATTERN = Pattern.compile(
            "^data:(image/[\\w.+-]+);base64,(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final CloudinaryService cloudinaryService;

    /**
     * Returns a final image URL for SOS record.
     * If input is base64 data URL, image is uploaded to Cloudinary and Cloudinary URL is returned.
     */
    public String resolveImageUrl(String rawImageUrl) {
        if (!StringUtils.hasText(rawImageUrl)) {
            return null;
        }

        String imageUrl = rawImageUrl.trim();
        if (isHttpUrl(imageUrl)) {
            if (isCloudinaryUrl(imageUrl)) {
                return imageUrl;
            }

            CloudinaryService.UploadedImage uploadedImage = cloudinaryService.uploadImageFromUrl(
                    imageUrl,
                    "sos-scene-" + UUID.randomUUID());
            return uploadedImage.url();
        }

        Matcher matcher = DATA_IMAGE_URL_PATTERN.matcher(imageUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "image_url must be a public URL or base64 data URL (data:image/...;base64,...)"
            );
        }

        String contentType = matcher.group(1);
        String base64Payload = matcher.group(2).replaceAll("\\s", "");
        byte[] imageBytes = decodeImage(base64Payload);

        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("image_url payload is too large. Maximum decoded size is 10MB");
        }

        CloudinaryService.UploadedImage uploadedImage = cloudinaryService.uploadImage(
                imageBytes,
                contentType,
                "sos-scene-" + UUID.randomUUID());

        return uploadedImage.url();
    }

    private byte[] decodeImage(String base64Payload) {
        try {
            return Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("image_url contains invalid base64 image data", ex);
        }
    }

    private boolean isHttpUrl(String imageUrl) {
        return imageUrl.startsWith("http://") || imageUrl.startsWith("https://");
    }

    private boolean isCloudinaryUrl(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String host = uri.getHost();
            return host != null && host.toLowerCase().contains("cloudinary.com");
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
