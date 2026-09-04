package com.frozenheart.backend.modules.socialinteraction.service;

import com.frozenheart.backend.modules.socialinteraction.dto.ReactRequest;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactResponseDto;

public interface ReactService {

    ReactResponseDto toggleReact(ReactRequest request);
}
