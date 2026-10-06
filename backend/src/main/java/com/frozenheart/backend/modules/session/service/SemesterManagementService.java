package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.CreateSemesterRequest;
import com.frozenheart.backend.modules.session.dto.SemesterResponse;

import java.util.List;

public interface SemesterManagementService {

    SemesterResponse createSemester(CreateSemesterRequest request);

    List<SemesterResponse> getAllSemesters();

    SemesterResponse activateSemester(Long semesterId);

    void deactivateAllSemesters();
 
    void deleteSemester(Long semesterId);
}
