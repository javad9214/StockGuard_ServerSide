package com.stockguard.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Startup self-check for the MinIO integration. Runs once the app is up:
 *
 * 1. probes the configured endpoint with plain HTTP to identify WHAT is
 *    actually listening there (MinIO answers /minio/health/live with 200 and
 *    identifies itself in the Server header; the MinIO console or any other
 *    service answers differently) — a wrong port is indistinguishable from a
 *    broken request once you only look at S3 errors;
 * 2. runs head → put → get → delete over a tiny probe object;
 * 3. retries the put with alternate request shapes (explicit content-length,
 *    chunked-signature client) in case only one framing is broken.
 *
 * Purely diagnostic: every step logs its own outcome and nothing here can
 * block startup.
 */
@Slf4j
@Component
public class S3Diagnostics {

    /** 1x1 transparent PNG (67 bytes). */
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private final S3Client s3Client;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    private final RestClient probeClient = RestClient.builder()
            .requestFactory(probeRequestFactory())
            .build();

    public S3Diagnostics(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        log.info("══════════ S3 DIAGNOSTICS START ══════════");
        try {
            probeEndpoint("/");
            probeEndpoint("/minio/health/live");

            byte[] png = Base64.getDecoder().decode(TINY_PNG_BASE64);
            String key = "diagnostics/probe.png";

            if (!step("headBucket", () ->
                    s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build()))) {
                return;
            }
            boolean anyPutWorked =
                    step("putObject/plain (current shape)", () ->
                            s3Client.putObject(
                                    PutObjectRequest.builder()
                                            .bucket(bucket).key(key).contentType("image/png")
                                            .build(),
                                    RequestBody.fromBytes(png)))
                    | step("putObject/with-explicit-content-length", () ->
                            s3Client.putObject(
                                    PutObjectRequest.builder()
                                            .bucket(bucket).key(key).contentType("image/png")
                                            .contentLength((long) png.length)
                                            .build(),
                                    RequestBody.fromBytes(png)))
                    | step("putObject/chunked-signature client", () ->
                            chunkedClient().putObject(
                                    PutObjectRequest.builder()
                                            .bucket(bucket).key(key).contentType("image/png")
                                            .build(),
                                    RequestBody.fromBytes(png)));
            if (anyPutWorked) {
                step("getObject", () -> {
                    try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(
                            GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                        log.info("🔧 S3-DIAG getObject: contentType={}, contentLength={}",
                                in.response().contentType(), in.response().contentLength());
                    }
                });
                step("deleteObject", () ->
                        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()));
            }
        } catch (Exception e) {
            log.warn("🔧 S3-DIAG unexpected: {}", e.toString(), e);
        } finally {
            log.info("══════════ S3 DIAGNOSTICS END ══════════");
        }
    }

    /** A diagnostic step that may throw anything. */
    @FunctionalInterface
    private interface Step {
        void run() throws Exception;
    }

    /** Logs the outcome of one step; true on success. */
    private boolean step(String name, Step call) {
        try {
            call.run();
            log.info("✅ S3-DIAG {} ok", name);
            return true;
        } catch (Exception e) {
            log.warn("❌ S3-DIAG {} failed: {}", name, e.toString(), e);
            return false;
        }
    }

    /** Plain unsigned GET, logging status/Server header/body prefix — reveals what the endpoint is. */
    private void probeEndpoint(String path) {
        try {
            probeClient.get().uri(URI.create(endpoint + path)).exchange((request, response) -> {
                String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                log.info("🔎 S3-DIAG GET {} -> {} server={} body={}",
                        path,
                        response.getStatusCode(),
                        response.getHeaders().getFirst("Server"),
                        abbreviate(body));
                return null;
            });
        } catch (Exception e) {
            log.warn("🔎 S3-DIAG GET {} failed: {}", path, e.toString());
        }
    }

    /** A client mirroring the pre-fix config (aws-chunked signing enabled). */
    private S3Client chunkedClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(true)
                        .build())
                .build();
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.substring(0, Math.min(200, compact.length()));
    }

    private static ClientHttpRequestFactory probeRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(3_000);
        return factory;
    }
}
