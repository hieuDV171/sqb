package com.frozenheart.backend.core.config;

import com.frozenheart.backend.modules.search.service.ElasticsearchIndexManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ElasticsearchInitializer implements CommandLineRunner {

    private final ElasticsearchIndexManager indexManager;

    @Override
    public void run(String @NonNull... args) {
        log.info("[ElasticsearchInitializer] 🚀 Bắt đầu quét và kiểm tra khởi tạo 6 Index trên Elasticsearch...");
        indexManager.initAllIndices();
        log.info("[ElasticsearchInitializer] ✅ Hoàn tất kiểm tra 6 Index trên Elasticsearch.");
    }
}
