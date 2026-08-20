package com.frozenheart.backend.modules.embedding.dto;

import java.util.List;

public record BatchEmbeddingResponse(
        List<List<Float>> embeddings,
        String model,
        int dimensions
) {

}
