package com.stockguard.service;

import com.stockguard.data.dto.common.StoredImage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageStorageService {

    /**
     * Stores an image in the MinIO bucket and returns the generated object key.
     *
     * @throws IllegalArgumentException if the file is empty, not an image or too large
     * @throws IOException              if reading the upload stream fails
     */
    String upload(MultipartFile file) throws IOException;

    /**
     * Streams a stored object back with its content type and length.
     *
     * @throws software.amazon.awssdk.services.s3.model.NoSuchKeyException if the key does not exist
     */
    StoredImage download(String key);

    /**
     * Deletes a stored object. Idempotent: deleting a missing key succeeds.
     */
    void delete(String key);
}
