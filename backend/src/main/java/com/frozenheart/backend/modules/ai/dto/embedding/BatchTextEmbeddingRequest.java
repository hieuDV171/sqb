package com.frozenheart.backend.modules.ai.dto.embedding;

import java.util.List;

public record BatchTextEmbeddingRequest(
        List<String> texts
    ) {

}
