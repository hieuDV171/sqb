package com.frozenheart.backend.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Bean
    public RestClient restClient() {
        return RestClient.builder(new HttpHost(host, port, "http")).build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonMapper jsonMapper) {
        return new RestClientTransport(restClient, new Jackson3JsonpMapper(jsonMapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        ElasticsearchClient client = new ElasticsearchClient(transport);

        // Kiểm tra kết nối nhanh khi khởi động ứng dụng
        try {
            boolean isConnected = client.ping().value();
            if (isConnected) {
                log.info("✅ Kết nối tới Elasticsearch thành công tại http://{}:{}", host, port);
            } else {
                log.warn("⚠️ Không thể ping tới Elasticsearch tại http://{}:{}", host, port);
            }
        } catch (Exception e) {
            log.warn("⚠️ Chưa thể kết nối tới Elasticsearch (Hãy chắc chắn Docker container đã chạy): {}", e.getMessage());
        }

        return client;
    }
}
