package com.frozenheart.backend.modules.embedding.dto;

import java.util.List;

public record EmbeddingResponse(
        List<Float> embedding,
        String model,
        int dimensions
) {

}
