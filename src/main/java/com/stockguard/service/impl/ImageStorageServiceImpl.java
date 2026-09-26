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
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 5_000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 10_000;

    private final S3Client s3Client;

    /**
     * Dedicated client for fetching external images (bounded timeouts so a
     * stalled third-party server can't hang request threads).
     */
    private final RestClient downloadClient = RestClient.builder()
            .requestFactory(downloadRequestFactory())
            .build();

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.image-max-size:10MB}")
    private DataSize maxImageSize;

    // checked lazily so a missing bucket doesn't block application startup
    private volatile boolean bucketReady;

    @Override
    public String upload(MultipartFile file) throws IOException {
        return upload(file.getBytes(), file.getContentType(), file.getOriginalFilename());
    }

    @Override
    public String upload(byte[] bytes, String contentType, String filename) {
        validate(bytes, contentType);
        ensureBucketExists();

        String key = buildKey(filename, contentType);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        log.info("Stored image '{}' ({} bytes, {}) in bucket '{}'",
                key, bytes.length, contentType, bucket);
        return key;
    }

    @Override
    public String storeFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("Image URL is blank");
        }

        ResponseEntity<byte[]> response = downloadClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .toEntity(byte[].class);

        byte[] bytes = response.getBody();
        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : null;

        return upload(bytes, contentType, filenameOf(imageUrl, contentType));
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

    private void validate(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only image files are accepted, received content type: " + contentType);
        }
        if (bytes.length > maxImageSize.toBytes()) {
            throw new IllegalArgumentException(
                    "Image exceeds the maximum allowed size of " + maxImageSize.toBytes() + " bytes");
        }
    }

    /**
     * Key = random UUID + sanitized original name (path stripped, ASCII-safe,
     * extension kept) so objects are unguessable yet identifiable in the bucket.
     */
    private String buildKey(String filename, String contentType) {
        String original = filename != null ? StringUtils.getFilename(filename) : null;
        String safe = original == null ? null : original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe != null && safe.length() > MAX_NAME_LENGTH) {
            safe = safe.substring(safe.length() - MAX_NAME_LENGTH); // keeps the extension
        }
        if (safe == null || safe.isBlank() || ".".equals(safe)) {
            safe = fallbackName(contentType);
        }
        return UUID.randomUUID() + "-" + safe;
    }

    /**
     * When the caller gives no usable filename (URL without a path, raw bytes),
     * name the object after the content type: "image.webp", "image.png", ...
     */
    private String fallbackName(String contentType) {
        String subtype = contentType != null && contentType.contains("/")
                ? contentType.substring(contentType.indexOf('/') + 1)
                : null;
        return "image" + (subtype != null && !subtype.isBlank() ? "." + subtype.replaceAll("[^a-zA-Z0-9]", "") : "");
    }

    /**
     * Filename taken from the URL path; query strings are excluded.
     */
    private String filenameOf(String imageUrl, String contentType) {
        try {
            String path = URI.create(imageUrl).getPath();
            String filename = path != null ? StringUtils.getFilename(path) : null;
            return StringUtils.hasText(filename) ? filename : fallbackName(contentType);
        } catch (IllegalArgumentException e) {
            return fallbackName(contentType);
        }
    }

    private static ClientHttpRequestFactory downloadRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS);
        return factory;
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
