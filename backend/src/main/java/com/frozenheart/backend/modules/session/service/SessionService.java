package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;

public interface SessionService {

    ProposeSessionResponse proposeSession(ProposeSessionRequest request);

}
