package com.stockguard.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the absolute URL of a stored image at request/response time.
 *
 * The database stores only the MinIO object key (never a domain/URL); the
 * base URL comes from the {@code app.base-url} property. Rows created before
 * this convention (SNAPP/Daryamart imports) hold full external URLs — those
 * are passed through unchanged so legacy images keep rendering.
 */
@Component
public class ImageUrlResolver {

    private static String baseUrl;

    @Value("${app.base-url}")
    private String configuredBaseUrl;

    @PostConstruct
    void init() {
        baseUrl = configuredBaseUrl;
    }

    /**
     * @param keyOrUrl stored object key, or a legacy/external absolute URL
     * @return the absolute URL to load the image from, or null/blank passthrough
     */
    public static String resolve(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) {
            return keyOrUrl;
        }
        // baseUrl may be unset in unit tests that never start the Spring context
        return (baseUrl != null ? baseUrl : "") + "/api/images/" + keyOrUrl;
    }

    /**
     * @return true when the stored value is a MinIO object key (i.e. owned by
     * this service and deletable), as opposed to a legacy/external full URL
     */
    public static boolean isStoredKey(String keyOrUrl) {
        return keyOrUrl != null && !keyOrUrl.isBlank()
                && !keyOrUrl.startsWith("http://") && !keyOrUrl.startsWith("https://");
    }
}
