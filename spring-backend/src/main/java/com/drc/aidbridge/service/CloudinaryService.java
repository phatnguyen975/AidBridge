package com.drc.aidbridge.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.drc.aidbridge.modules.shared.exception.CloudinaryOperationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Encapsulates all Cloudinary file operations used by the API layer.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final String IMAGE_RESOURCE_TYPE = "image";
    private static final String DELETE_RESULT_OK = "ok";
    private static final String DELETE_RESULT_NOT_FOUND = "not found";

    private final Cloudinary cloudinary;

    public UploadResponse uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", IMAGE_RESOURCE_TYPE,
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false));

            return mapUploadResponse(uploadResult);
        } catch (IOException ex) {
            throw new CloudinaryOperationException("Failed to read uploaded image", ex);
        } catch (RuntimeException ex) {
            throw new CloudinaryOperationException("Failed to upload image to Cloudinary", ex);
        }
    }

    public void deleteImage(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            throw new IllegalArgumentException("publicId must not be blank");
        }

        try {
            Map<?, ?> deletionResult = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", IMAGE_RESOURCE_TYPE,
                            "invalidate", true));

            String result = asString(deletionResult.get("result"));
            if (DELETE_RESULT_NOT_FOUND.equalsIgnoreCase(result)) {
                throw new ResourceNotFoundException("Image not found for publicId: " + publicId);
            }
            if (!DELETE_RESULT_OK.equalsIgnoreCase(result)) {
                throw new CloudinaryOperationException("Failed to delete image from Cloudinary");
            }
        } catch (IOException ex) {
            throw new CloudinaryOperationException("Failed to communicate with Cloudinary", ex);
        } catch (ResourceNotFoundException | CloudinaryOperationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CloudinaryOperationException("Failed to delete image from Cloudinary", ex);
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

    private UploadResponse mapUploadResponse(Map<?, ?> uploadResult) {
        String secureUrl = asString(uploadResult.get("secure_url"));
        if (!StringUtils.hasText(secureUrl)) {
            throw new CloudinaryOperationException("Cloudinary did not return a secure URL");
        }

        return new UploadResponse(
                secureUrl,
                asString(uploadResult.get("public_id")),
                asString(uploadResult.get("format")),
                asString(uploadResult.get("resource_type")));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public record UploadResponse(
            String url,
            @JsonProperty("public_id") String publicId,
            String format,
            @JsonProperty("resource_type") String resourceType) {
    }
}
