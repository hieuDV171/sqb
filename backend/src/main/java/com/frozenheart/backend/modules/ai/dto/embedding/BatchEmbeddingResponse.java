package com.frozenheart.backend.modules.ai.dto.embedding;

import java.util.List;

public record BatchEmbeddingResponse(
        List<List<Float>> embeddings,
        String model,
        int dimensions
) {

}
