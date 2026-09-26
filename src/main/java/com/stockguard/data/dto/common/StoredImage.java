package com.stockguard.data.dto.common;

import java.io.InputStream;

/**
 * An object streamed back from S3/MinIO together with the metadata needed
 * to serve it over HTTP.
 */
public record StoredImage(InputStream content, String contentType, long contentLength) {
}
