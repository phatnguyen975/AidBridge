package com.drc.aidbridge.modules.attachment.internal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.drc.aidbridge.modules.shared.exception.CloudinaryOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final String IMAGE_RESOURCE_TYPE = "image";

    private final Cloudinary cloudinary;

    public UploadedImage uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", IMAGE_RESOURCE_TYPE,
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false));

            return mapUploadedImage(uploadResult);
        } catch (IOException ex) {
            throw new CloudinaryOperationException("Failed to read uploaded image", ex);
        } catch (RuntimeException ex) {
            throw new CloudinaryOperationException("Failed to upload image to Cloudinary", ex);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }

    private UploadedImage mapUploadedImage(Map<?, ?> uploadResult) {
        String secureUrl = asString(uploadResult.get("secure_url"));
        String publicId = asString(uploadResult.get("public_id"));
        if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
            throw new CloudinaryOperationException("Cloudinary did not return the required upload metadata");
        }

        return new UploadedImage(secureUrl, publicId);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public record UploadedImage(String url, String publicId) {
    }
}
