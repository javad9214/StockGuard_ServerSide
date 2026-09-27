package com.stockguard.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Slf4j
@Configuration
public class S3Config {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        // startup marker: proves from the logs which S3 settings a deployed
        // instance actually runs with
        log.info("S3 client for {}: pathStyle=true, chunkedEncoding=false", endpoint);
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // required by SDK, MinIO ignores it
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // required for MinIO
                        // By default the SDK signs PutObject with aws-chunked
                        // framing (STREAMING-AWS4-HMAC-SHA256-PAYLOAD), which
                        // MinIO rejects with a bare 400 Bad Request. Disabling
                        // chunked encoding sends a plain signed body instead.
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}