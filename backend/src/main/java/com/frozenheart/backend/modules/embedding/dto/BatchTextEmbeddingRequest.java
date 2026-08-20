package com.frozenheart.backend.modules.embedding.dto;

import java.util.List;

public record BatchTextEmbeddingRequest(
        List<String> texts
    ) {

}
