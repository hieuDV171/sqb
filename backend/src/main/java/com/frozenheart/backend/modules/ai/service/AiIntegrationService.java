package com.frozenheart.backend.modules.ai.service;

import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiApplyResponse;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;

public interface AiIntegrationService {

    AiRefineResponse refineQuestion(Long questionId, AiRefineRequest request, EditActorType actorType);

    AiApplyResponse applyAiRefinement(Long questionId, AiApplyRequest request);
}
