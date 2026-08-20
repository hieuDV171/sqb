package com.frozenheart.backend.modules.session.service;

public interface DuplicateDetectionService {
    
    void asyncCheckDuplicates(Long sessionId);


}
