package com.frozenheart.backend.modules.ai.service.impl;

import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarRequest;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarResponse;
import com.frozenheart.backend.modules.ai.service.AiSidecarClientService;
import com.frozenheart.backend.modules.embedding.dto.BatchEmbeddingResponse;
import com.frozenheart.backend.modules.embedding.dto.BatchTextEmbeddingRequest;
import com.frozenheart.backend.modules.embedding.dto.EmbeddingResponse;
import com.frozenheart.backend.modules.embedding.dto.TextEmbeddingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSidecarClientServiceImpl implements AiSidecarClientService {

    private final RestClient aiSidecarRestClient;
    private final MetricService metricService;

    @Override
    public float[] getSingleTextEmbedding(String text) {
        if (text == null || text.isBlank())
            return new float[0];

        return metricService.recordTime("sqb.ai.embedding.duration", () -> {
            try {
                EmbeddingResponse response = aiSidecarRestClient.post()
                        .uri("/api/v1/embed/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new TextEmbeddingRequest(text))
                        .retrieve()
                        .body(EmbeddingResponse.class);

                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "text", "status", "success");
                return convertToFloatArray(response != null ? response.embedding() : null);

            } catch (Exception e) {
                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "text", "status", "failed");
                log.error("[AiSidecarClient] Lỗi khi tạo Text Embedding: ", e);
                return new float[0];
            }
        }, "type", "text");
    }

    @Override
    public List<float[]> getBatchTextEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty())
            return Collections.emptyList();

        return metricService.recordTime("sqb.ai.embedding.duration", () -> {
            try {
                BatchEmbeddingResponse response = aiSidecarRestClient.post()
                        .uri("/api/v1/embed/text/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new BatchTextEmbeddingRequest(texts))
                        .retrieve()
                        .body(BatchEmbeddingResponse.class);

                if (response == null || response.embeddings() == null)
                    return Collections.emptyList();

                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "batch_text", "status", "success");
                return response.embeddings().stream()
                        .map(this::convertToFloatArray)
                        .toList();

            } catch (Exception e) {
                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "batch_text", "status", "failed");
                log.error("[AiSidecarClient] Lỗi khi tạo Batch Text Embedding: ", e);
                return Collections.emptyList();
            }
        }, "type", "batch_text");
    }

    @Override
    public float[] getImageEmbedding(byte[] imageBytes, String filename) {
        if (imageBytes == null || imageBytes.length == 0)
            return new float[0];

        return metricService.recordTime("sqb.ai.embedding.duration", () -> {
            try {
                MultipartBodyBuilder body = getMultipartBodyBuilder(imageBytes, filename);

                EmbeddingResponse response = aiSidecarRestClient.post()
                        .uri("/api/v1/embed/image")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body.build())
                        .retrieve()
                        .body(EmbeddingResponse.class);

                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "image", "status", "success");
                return convertToFloatArray(response != null ? response.embedding() : null);

            } catch (Exception e) {
                metricService.incrementCounter("sqb.ai.embedding.requests", "type", "image", "status", "failed");
                log.error("[AiSidecarClient] Lỗi khi tạo Image Embedding: ", e);
                return new float[0];
            }
        }, "type", "image");
    }

    @Override
    public BtpropSidecarResponse auditQuestion(BtpropSidecarRequest request) {
        return metricService.recordTime("sqb.ai.btprop.duration", () -> {
            try {
                BtpropSidecarResponse response = aiSidecarRestClient.post()
                        .uri("/api/v1/ai/btprop/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(BtpropSidecarResponse.class);

                metricService.incrementCounter("sqb.ai.btprop.requests", "status", "success");
                return response;

            } catch (Exception e) {
                metricService.incrementCounter("sqb.ai.btprop.requests", "status", "failed");
                log.error("[AiSidecarClient] Lỗi khi gọi BTProp Audit: {}", e.getMessage(), e);
                return null;
            }
        });
    }

    private @NonNull MultipartBodyBuilder getMultipartBodyBuilder(byte[] imageBytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "image.jpg";
            }
        };

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", resource);
        return body;
    }

    private float[] convertToFloatArray(List<Float> list) {
        if (list == null)
            return new float[0];

        int size = list.size();
        float[] floatArray = new float[size];
        for (int i = 0; i < size; i++) {
            floatArray[i] = list.get(i);
        }
        return floatArray;
    }
}
