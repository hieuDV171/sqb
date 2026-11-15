package com.frozenheart.backend.modules.media.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.modules.media.service.impl.R2AccountUsage.ObjectRef;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class R2AccountUsageTest {
    private static final String ACCOUNT = "ff0000000000000000000000000000aa";
    private static final String BASE = "https://cloudflare.test/client/v4";

    @Test
    void combinesPaginatedInventoryAcrossBucketsWithAccountMultipartUsage() {
        var properties = new R2Properties();
        properties.setAccountId(ACCOUNT);
        properties.setEndpoint("https://" + ACCOUNT + ".r2.cloudflarestorage.com");
        properties.setMetricsApiToken("read-token-fixture");
        properties.setBucketName("sqb-public");
        properties.setReportEvidenceBucket("sqb-secret");
        var builder = RestClient.builder().baseUrl(BASE);
        var server = MockRestServiceServer.bindTo(builder).build();

        expect(server, "/buckets?per_page=1000", """
                {"result":{"buckets":[{"name":"sqb-public"}]},"result_info":{"cursor":"next"}}
                """);
        expect(server, "/buckets/sqb-public/objects?per_page=1000", """
                {"result":[{"key":"uploads/a","size":100}],"result_info":{"cursor":"next+/=","is_truncated":true}}
                """);
        expect(server, "/buckets/sqb-public/objects?per_page=1000&cursor=next%2B%2F%3D", """
                {"result":[{"key":"uploads/b","size":200}],"result_info":{"is_truncated":false}}
                """);
        expect(server, "/buckets?per_page=1000&cursor=next", """
                {"result":{"buckets":[{"name":"sqb-secret"},{"name":"other-bucket"}]}}
                """);
        expect(server, "/buckets/sqb-secret/objects?per_page=1000", """
                {"result":[{"key":"exports/report","size":300}]}
                """);
        expect(server, "/buckets/other-bucket/objects?per_page=1000", """
                {"result":[{"key":"unrelated-data","size":400}]}
                """);
        expect(server, "/metrics", """
                {"result":{"standard":{"published":{"payloadSize":900},"uploaded":{"payloadSize":50,"metadataSize":5}}}}
                """);

        var tracked = new ObjectRef("sqb-public", "uploads/b");
        var result = new R2AccountUsage(properties, builder.build()).read(Set.of(tracked));

        assertEquals(1055, result.bytes());
        assertEquals(Map.of(tracked, 200L), result.observed());
        server.verify();
    }

    private void expect(MockRestServiceServer server, String path, String body) {
        server.expect(requestTo(BASE + "/accounts/" + ACCOUNT + "/r2" + path))
                .andExpect(header("Authorization", "Bearer read-token-fixture"))
                .andRespond(withSuccess("{\"success\":true," + body.strip().substring(1), MediaType.APPLICATION_JSON));
    }
}
