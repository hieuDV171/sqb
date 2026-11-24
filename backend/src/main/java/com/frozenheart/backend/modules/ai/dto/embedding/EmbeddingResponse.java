package com.frozenheart.backend.modules.ai.dto.embedding;

import java.util.List;

public record EmbeddingResponse(
        List<Float> embedding,
        String model,
        int dimensions
) {

}
