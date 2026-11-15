package com.frozenheart.backend.modules.media.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class R2AccountUsage {
    private final R2Properties properties;
    private final RestClient client;

    @Autowired
    public R2AccountUsage(R2Properties properties) {
        this(properties, createClient());
    }

    R2AccountUsage(R2Properties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    private static RestClient createClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("https://api.cloudflare.com/client/v4").requestFactory(factory).build();
    }

    public Usage read(Set<ObjectRef> tracked) {
        validateConfiguration();
        long inventoryBytes = 0;
        var observed = new HashMap<ObjectRef, Long>();
        var buckets = new HashSet<String>();
        for (String jurisdiction : Set.copyOf(properties.getQuotaJurisdictions())) {
            String cursor = null;
            do {
                JsonNode page = get(jurisdiction, cursor, "buckets");
                for (JsonNode node : array(page.path("result").path("buckets"))) {
                    String bucket = requiredString(node.path("name"));
                    if (buckets.add(bucket)) {
                        inventoryBytes = Math.addExact(inventoryBytes, inventory(jurisdiction, bucket, tracked, observed));
                    }
                }
                cursor = nextCursor(page.path("result_info"), cursor);
            } while (cursor != null);
        }
        if (!buckets.contains(properties.getBucketName()) || (properties.getReportEvidenceBucket() != null
                && !properties.getReportEvidenceBucket().isBlank() && !buckets.contains(properties.getReportEvidenceBucket()))) {
            throw new IllegalStateException("An SQB bucket is missing from the account inventory");
        }
        JsonNode metrics = get(null, null, "metrics").path("result");
        long published = Math.max(inventoryBytes, metricBytes(metrics, "published"));
        return new Usage(Math.addExact(published, metricBytes(metrics, "uploaded")), observed);
    }

    private long inventory(String jurisdiction, String bucket, Set<ObjectRef> tracked, Map<ObjectRef, Long> observed) {
        long total = 0;
        String cursor = null;
        do {
            var page = get(jurisdiction, cursor, "buckets", bucket, "objects");
            for (var object : array(page.path("result"))) {
                long bytes = requiredBytes(object.path("size"));
                total = Math.addExact(total, bytes);
                var ref = new ObjectRef(bucket, requiredString(object.path("key")));
                if (tracked.contains(ref)) observed.put(ref, bytes);
            }
            cursor = nextCursor(page.path("result_info"), cursor);
        } while (cursor != null);
        return total;
    }

    private JsonNode get(String jurisdiction, String cursor, String... path) {
        var request = client.get().uri(uri -> {
            uri.pathSegment("accounts", properties.getAccountId(), "r2").pathSegment(path);
            if (jurisdiction != null) uri.queryParam("per_page", 1000);
            if (cursor != null) uri.queryParam("cursor", "{cursor}");
            return cursor == null ? uri.build() : uri.build(cursor);
        }).headers(headers -> headers.setBearerAuth(properties.getMetricsApiToken()));
        if (jurisdiction != null) request.header("cf-r2-jurisdiction", jurisdiction);
        JsonNode body = request.retrieve().body(JsonNode.class);
        if (body == null || !body.path("success").asBoolean(false)) {
            throw new IllegalStateException("Cloudflare did not return a successful R2 usage response");
        }
        return body;
    }

    private static long metricBytes(JsonNode result, String state) {
        if (!result.isObject()) throw new IllegalStateException("Invalid R2 metrics result");
        long total = 0;
        for (String storageClass : Set.of("standard", "infrequentAccess")) {
            JsonNode usage = result.path(storageClass).path(state);
            for (String field : Set.of("payloadSize", "metadataSize")) {
                JsonNode bytes = usage.path(field);
                if (!bytes.isMissingNode() && !bytes.isNull()) {
                    total = Math.addExact(total, requiredBytes(bytes));
                }
            }
        }
        return total;
    }

    private static long requiredBytes(JsonNode size) {
        if (!size.isNumber() || size.asLong() < 0 || !size.canConvertToLong()) {
            throw new IllegalStateException("Invalid R2 object size");
        }
        return size.asLong();
    }

    private static JsonNode array(JsonNode node) {
        if (!node.isArray()) throw new IllegalStateException("Invalid R2 inventory response");
        return node;
    }

    private static String requiredString(JsonNode node) {
        if (!node.isString() || node.asString().isBlank()) throw new IllegalStateException("Invalid R2 inventory entry");
        return node.asString();
    }

    private static String nextCursor(JsonNode info, String previous) {
        String cursor = info.path("cursor").asString("");
        if (info.path("is_truncated").isBoolean() && !info.path("is_truncated").asBoolean()) return null;
        if (cursor.isBlank() && !info.path("is_truncated").asBoolean(false)) return null;
        if (cursor.isBlank() || cursor.equals(previous)) throw new IllegalStateException("Invalid R2 inventory cursor");
        return cursor;
    }

    private void validateConfiguration() {
        String account = properties.getAccountId();
        String token = properties.getMetricsApiToken();
        if (account == null || !account.matches("[0-9a-fA-F]{32}") || token == null || token.isBlank()) {
            throw new IllegalStateException("R2_ACCOUNT_ID and R2_METRICS_API_TOKEN must be configured");
        }
        String host = URI.create(properties.getEndpoint()).getHost();
        if (host == null || !host.startsWith(account + ".") || !host.endsWith(".r2.cloudflarestorage.com")) {
            throw new IllegalStateException("R2_ACCOUNT_ID must match R2_ENDPOINT");
        }
        if (properties.getQuotaJurisdictions().isEmpty() || properties.getStorageWarningBytes() <= 0
                || properties.getStorageWarningBytes() >= properties.getStorageLimitBytes()) {
            throw new IllegalStateException("Invalid R2 quota configuration");
        }
    }

    public record ObjectRef(String bucket, String key) {}

    public record Usage(long bytes, Map<ObjectRef, Long> observed) {}
}
