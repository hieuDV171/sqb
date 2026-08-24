package com.frozenheart.backend.modules.ai.service;

import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatHistoryResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatRequest;
import com.frozenheart.backend.modules.ai.dto.AiChatResponse;
import com.frozenheart.backend.modules.ai.dto.AiChatSessionSummaryResponse;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;

public interface AiIntegrationService {

    AiRefineResponse refineQuestion(Long questionId, AiRefineRequest request, EditActorType actorType);

    AiChatResponse chatWithAi(AiChatRequest request);

    AiApplyResponse applyAiRefinement(AiApplyRequest request);

    CursorResponse<AiChatHistoryResponse> getChatHistory(String sessionId, Long after, int limit);

    CursorResponse<AiChatSessionSummaryResponse> getUserChatSessions(Long after, int limit);
}
