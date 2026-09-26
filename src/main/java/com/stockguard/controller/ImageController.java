package com.stockguard.controller;

import com.stockguard.data.dto.common.ResponseDTO;
import com.stockguard.data.dto.common.StoredImage;
import com.stockguard.data.dto.image.response.ImageUploadResponseDTO;
import com.stockguard.data.enums.ResponseCode;
import com.stockguard.service.ImageStorageService;
import com.stockguard.util.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageController extends BaseController {

    private final ImageStorageService imageStorageService;

    /**
     * Download a stored image. Public (no auth) so the app can load images
     * with plain Coil/Glide — keys are unguessable random UUIDs.
     * GET /api/images/{key}
     */
    @GetMapping("/api/images/{key}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String key) {
        try {
            StoredImage image = imageStorageService.download(key);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .contentLength(Math.max(image.contentLength(), 0))
                    .body(new InputStreamResource(image.content()));
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to download image '{}'", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upload an image (admin only).
     * POST /api/admin/images/upload
     *
     * @return the stored object key plus the absolute URL the image is
     *         served from (built from app.base-url — never persisted)
     */
    @PostMapping("/api/admin/images/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<ImageUploadResponseDTO>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String key = imageStorageService.upload(file);
            return generateOKResponse(new ImageUploadResponseDTO(key, ImageUrlResolver.resolve(key)));
        } catch (IllegalArgumentException e) {
            return generateErrorResponse(HttpStatus.BAD_REQUEST, ResponseCode.VALIDATION_ERROR, e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read uploaded image", e);
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, "Failed to store image");
        }
    }

    /**
     * Delete a stored image (admin only).
     * DELETE /api/admin/images/{key}
     */
    @DeleteMapping("/api/admin/images/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseDTO<Void>> delete(@PathVariable String key) {
        try {
            imageStorageService.delete(key);
            return generateOKResponse(null);
        } catch (Exception e) {
            log.error("Failed to delete image '{}'", key, e);
            return generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ResponseCode.INTERNAL_ERROR, "Failed to delete image");
        }
    }
}
