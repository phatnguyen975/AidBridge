package com.drc.aidbridge.controller;

import com.drc.aidbridge.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for uploading and deleting image files in Cloudinary.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CloudinaryService.UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {
        CloudinaryService.UploadResponse response = cloudinaryService.uploadImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteImage(@RequestParam("publicId") String publicId) {
        cloudinaryService.deleteImage(publicId);
        return ResponseEntity.noContent().build();
    }
}
