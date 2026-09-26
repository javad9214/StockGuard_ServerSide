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
     * Stores raw image bytes in the MinIO bucket and returns the generated
     * object key. Same validation rules as {@link #upload(MultipartFile)}.
     *
     * @throws IllegalArgumentException if the bytes are empty, not an image or too large
     */
    String upload(byte[] bytes, String contentType, String filename);

    /**
     * Downloads an external image (e.g. a Daryamart product photo) and stores
     * it in the MinIO bucket, returning the generated object key — so the DB
     * keeps owning the image instead of hotlinking a third-party URL.
     *
     * @throws IllegalArgumentException if the URL is blank, the download is
     *                                  empty, not an image or too large
     * @throws org.springframework.web.client.RestClientException if the download fails
     */
    String storeFromUrl(String imageUrl);

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
