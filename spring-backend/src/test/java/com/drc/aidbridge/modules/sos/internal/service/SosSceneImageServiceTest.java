package com.drc.aidbridge.modules.sos.internal.service;

import com.drc.aidbridge.modules.attachment.internal.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SosSceneImageServiceTest {

    private CloudinaryService cloudinaryService;
    private SosSceneImageService sosSceneImageService;

    @BeforeEach
    void setUp() {
        cloudinaryService = mock(CloudinaryService.class);
        sosSceneImageService = new SosSceneImageService(cloudinaryService);
    }

    @Test
    void resolveImageUrl_ShouldReturnNull_WhenInputIsBlank() {
        assertNull(sosSceneImageService.resolveImageUrl(null));
        assertNull(sosSceneImageService.resolveImageUrl("   "));
    }

    @Test
    void resolveImageUrl_ShouldReturnSameUrl_WhenInputIsHttpUrl() {
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/sos.jpg";

        String resolvedUrl = sosSceneImageService.resolveImageUrl(imageUrl);

        assertEquals(imageUrl, resolvedUrl);
    }

    @Test
    void resolveImageUrl_ShouldUploadToCloudinary_WhenInputIsExternalHttpUrl() {
        String externalUrl = "https://example.com/scene.jpg";

        when(cloudinaryService.uploadImageFromUrl(eq(externalUrl), anyString()))
                .thenReturn(new CloudinaryService.UploadedImage(
                        "https://res.cloudinary.com/demo/image/upload/v1/sos-external.jpg",
                        "sos_external_public_id"
                ));

        String resolvedUrl = sosSceneImageService.resolveImageUrl(externalUrl);

        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/sos-external.jpg", resolvedUrl);
        verify(cloudinaryService).uploadImageFromUrl(eq(externalUrl), anyString());
    }

    @Test
    void resolveImageUrl_ShouldUploadToCloudinary_WhenInputIsDataUrl() {
        byte[] imageBytes = "fake-image-content".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:image/png;base64," + base64;

        when(cloudinaryService.uploadImage(any(byte[].class), eq("image/png"), anyString()))
                .thenReturn(new CloudinaryService.UploadedImage(
                        "https://res.cloudinary.com/demo/image/upload/v1/sos.png",
                        "sos_public_id"
                ));

        String resolvedUrl = sosSceneImageService.resolveImageUrl(dataUrl);

        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/sos.png", resolvedUrl);

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cloudinaryService).uploadImage(bytesCaptor.capture(), eq("image/png"), anyString());
        assertArrayEquals(imageBytes, bytesCaptor.getValue());
    }

    @Test
    void resolveImageUrl_ShouldThrowIllegalArgument_WhenInputIsNotSupported() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sosSceneImageService.resolveImageUrl("content://media/external/images/media/123")
        );

        assertEquals(
                "image_url must be a public URL or base64 data URL (data:image/...;base64,...)",
                exception.getMessage()
        );
    }
}
