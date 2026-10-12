package com.flashsale.orderprocessor;

import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderObservability {
    private static final Logger LOG = Logger.getLogger(OrderObservability.class);
    public static final String API_VERSION = "v1";
    public static final String MESSAGE_SCHEMA_VERSION = "1.0";
    private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<String, String> TRACE_IDS = new ConcurrentHashMap<>();

    private OrderObservability() {
    }

    public static String schemaVersion() {
        return MESSAGE_SCHEMA_VERSION;
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }

    public static String traceIdFor(String pedidoId) {
        if (pedidoId == null || pedidoId.isBlank()) {
            return newTraceId();
        }
        return TRACE_IDS.computeIfAbsent(pedidoId, ignored -> newTraceId());
    }

    public static void registerTrace(String pedidoId) {
        if (pedidoId != null && !pedidoId.isBlank()) {
            TRACE_IDS.putIfAbsent(pedidoId, newTraceId());
        }
    }

    public static void reset() {
        COUNTERS.clear();
        TRACE_IDS.clear();
    }

    public static void record(String metricName, long delta) {
        if (metricName == null || metricName.isBlank()) {
            return;
        }
        COUNTERS.computeIfAbsent(metricName, ignored -> new AtomicLong()).addAndGet(delta);
    }

    public static void recordRetry(String scope, int attempt) {
        record("retry." + scope, attempt);
    }

    public static void recordLookup(boolean hit) {
        record(hit ? "cache.hit" : "cache.miss", 1L);
    }

    public static void logEvent(String eventName, String pedidoId, String status, Map<String, Object> details) {
        LOG.info(formatEvent(eventName, pedidoId, status, details));
    }

    private static String formatEvent(String eventName, String pedidoId, String status, Map<String, Object> details) {
        return formatContext(eventContext(eventName, pedidoId, status, details));
    }

    private static Map<String, Object> eventContext(String eventName, String pedidoId, String status, Map<String, Object> details) {
        Map<String, Object> context = baseContext(eventName, pedidoId, status);
        addDetails(context, details);
        context.put("trace_id", traceIdFor(pedidoId));
        return context;
    }

    private static Map<String, Object> baseContext(String eventName, String pedidoId, String status) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", eventName);
        context.put("pedido_id", pedidoId);
        context.put("status", status);
        return context;
    }

    private static void addDetails(Map<String, Object> context, Map<String, Object> details) {
        if (details != null) {
            context.putAll(details);
        }
    }

    private static String formatContext(Map<String, Object> context) {
        StringBuilder message = new StringBuilder();
        context.forEach((key, value) -> appendContextEntry(message, key, value));
        return message.toString();
    }

    private static void appendContextEntry(StringBuilder message, String key, Object value) {
        if (message.length() > 0) {
            message.append(' ');
        }
        message.append(key).append('=').append(value);
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        COUNTERS.forEach((name, counter) -> snapshot.put(name, counter.get()));
        snapshot.put("trace_ids", TRACE_IDS.size());
        return snapshot;
    }
}
