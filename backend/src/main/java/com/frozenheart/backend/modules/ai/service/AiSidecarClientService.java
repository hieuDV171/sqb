package com.frozenheart.backend.modules.ai.service;

import com.frozenheart.backend.modules.ai.dto.BtpropSidecarRequest;
import com.frozenheart.backend.modules.ai.dto.BtpropSidecarResponse;

import java.util.List;

public interface AiSidecarClientService {
    float[] getSingleTextEmbedding(String text);
    List<float[]> getBatchTextEmbeddings(List<String> texts);
    float[] getImageEmbedding(byte[] imageBytes, String filename);
    BtpropSidecarResponse auditQuestion(BtpropSidecarRequest request);
}
