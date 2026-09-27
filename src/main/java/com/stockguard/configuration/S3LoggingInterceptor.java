package com.stockguard.configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logs every S3 call end to end — request object, wire headers, response
 * status, and failures with their stack — while the MinIO integration is
 * being debugged. Authorization values are truncated; they are per-request
 * signatures for localhost traffic, but there is no reason to log them whole.
 */
@Slf4j
public class S3LoggingInterceptor implements ExecutionInterceptor {

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attributes) {
        log.info("🔧 S3 >>> {} {}",
                attributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME),
                context.request());
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
        log.info("🔧 S3 >>--> {} {} headers={}",
                context.httpRequest().method(),
                context.httpRequest().getUri(),
                redact(context.httpRequest().headers()));
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes attributes) {
        log.info("🔧 S3 <-- status={} headers={}",
                context.httpResponse().statusCode(),
                context.httpResponse().headers());
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes attributes) {
        log.warn("🔧 S3 !!! {} failed",
                attributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME),
                context.exception());
    }

    private Map<String, List<String>> redact(Map<String, List<String>> headers) {
        // the SDK hands out an unmodifiable map — copy, never mutate
        Map<String, List<String>> copy = new LinkedHashMap<>(headers);
        List<String> auth = copy.get("Authorization");
        if (auth != null && !auth.isEmpty()) {
            String v = auth.get(0);
            copy.put("Authorization", List.of(v.substring(0, Math.min(30, v.length())) + "…"));
        }
        return copy;
    }
}
