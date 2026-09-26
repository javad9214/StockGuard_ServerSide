package com.stockguard.service.impl;

import com.stockguard.data.dto.common.StoredImage;
import com.stockguard.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final int MAX_NAME_LENGTH = 80;

    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.image-max-size:10MB}")
    private DataSize maxImageSize;

    // checked lazily so a missing bucket doesn't block application startup
    private volatile boolean bucketReady;

    @Override
    public String upload(MultipartFile file) throws IOException {
        validate(file);
        ensureBucketExists();

        String key = buildKey(file);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        log.info("Stored image '{}' ({} bytes, {}) in bucket '{}'",
                key, file.getSize(), file.getContentType(), bucket);
        return key;
    }

    @Override
    public StoredImage download(String key) {
        ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
        GetObjectResponse response = stream.response();
        return new StoredImage(stream, response.contentType(), response.contentLength() == null ? -1 : response.contentLength());
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
        log.info("Deleted image '{}' from bucket '{}'", key, bucket);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only image files are accepted, received content type: " + file.getContentType());
        }
        if (file.getSize() > maxImageSize.toBytes()) {
            throw new IllegalArgumentException(
                    "Image exceeds the maximum allowed size of " + maxImageSize.toBytes() + " bytes");
        }
    }

    /**
     * Key = random UUID + sanitized original name (path stripped, ASCII-safe,
     * extension kept) so objects are unguessable yet identifiable in the bucket.
     */
    private String buildKey(MultipartFile file) {
        String original = StringUtils.getFilename(file.getOriginalFilename());
        String safe = original == null ? "image" : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > MAX_NAME_LENGTH) {
            safe = safe.substring(safe.length() - MAX_NAME_LENGTH); // keeps the extension
        }
        if (safe.isBlank() || ".".equals(safe)) {
            safe = "image";
        }
        return UUID.randomUUID() + "-" + safe;
    }

    private synchronized void ensureBucketExists() {
        if (bucketReady) {
            return;
        }
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' does not exist, creating it", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }
}
