package com.frozenheart.backend.modules.embedding.service.impl;

import com.frozenheart.backend.modules.embedding.dto.*;
import com.frozenheart.backend.modules.embedding.service.EmbeddingClientService;
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
public class EmbeddingClientServiceImpl implements EmbeddingClientService {

    private final RestClient embeddingRestClient;

    @Override
    public float[] getSingleTextEmbedding(String text) {
        if (text == null || text.isBlank())
            return new float[0];

        try {
            EmbeddingResponse response = embeddingRestClient.post()
                    .uri("/embed/text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TextEmbeddingRequest(text))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            return convertToFloatArray(response != null ? response.embedding() : null);

        } catch (Exception e) {
            log.error("[EmbeddingClient] Lỗi khi tạo Text Embedding: ", e);
            return new float[0];
        }
    }

    @Override
    public List<float[]> getBatchTextEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty())
            return Collections.emptyList();

        try {
            BatchEmbeddingResponse response = embeddingRestClient.post()
                    .uri("/embed/text/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new BatchTextEmbeddingRequest(texts))
                    .retrieve()
                    .body(BatchEmbeddingResponse.class);

            if (response == null || response.embeddings() == null)
                return Collections.emptyList();

            return response.embeddings().stream()
                    .map(this::convertToFloatArray)
                    .toList();

        } catch (Exception e) {
            log.error("[EmbeddingClient] Lỗi khi tạo Batch Text Embedding: ", e);
            return Collections.emptyList();
        }
    }

    @Override
    public float[] getImageEmbedding(byte[] imageBytes, String filename) {
        if (imageBytes == null || imageBytes.length == 0)
            return new float[0];

        try {
            MultipartBodyBuilder body = getMultipartBodyBuilder(imageBytes, filename);

            EmbeddingResponse response = embeddingRestClient.post()
                    .uri("/embed/image")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(EmbeddingResponse.class);

            return convertToFloatArray(response != null ? response.embedding() : null);

        } catch (Exception e) {
            log.error("[EmbeddingClient] Lỗi khi tạo Image Embedding: ", e);
            return new float[0];
        }
    }

    private @NonNull MultipartBodyBuilder getMultipartBodyBuilder(byte[] imageBytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "image.jpg";
            }
        };

        // Cách trực tiếp nhất nói với Spring body là multipart
        // MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // body.add("file", resource)

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
