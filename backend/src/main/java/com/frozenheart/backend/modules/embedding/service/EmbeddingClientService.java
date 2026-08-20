package com.frozenheart.backend.modules.embedding.service;

import java.util.List;

public interface EmbeddingClientService {
    float[] getSingleTextEmbedding(String text);

    List<float[]> getBatchTextEmbeddings(List<String> texts);

    float[] getImageEmbedding(byte[] imageBytes, String filename);
}
