package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.dto.CreateSemesterRequest;
import com.frozenheart.backend.modules.session.dto.SemesterResponse;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.session.service.SemesterManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterManagementServiceImpl implements SemesterManagementService {

    private final SemesterRepository semesterRepository;
    private final CurrentSemesterHolder currentSemesterHolder;

    @Override
    @Transactional
    public SemesterResponse createSemester(CreateSemesterRequest request) {
        Semester semester = Semester.builder()
                .name(request.name().trim())
                .active(false)
                .build();

        Semester saved = semesterRepository.save(semester);
        log.info("[SemesterManagement] Created new semester id={}, name={}", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public SemesterResponse activateSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy học kỳ"));

        // Reset toàn bộ học kỳ khác về active = false
        semesterRepository.deactivateAllSemesters();

        // Kích hoạt học kỳ được chọn
        semester.setActive(true);
        Semester saved = semesterRepository.save(semester);

        // Đồng bộ cache RAM ngay lập tức
        currentSemesterHolder.setCurrentSemester(saved);

        log.info("[SemesterManagement] Activated semester id={}, name={}", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateAllSemesters() {
        semesterRepository.deactivateAllSemesters();
        currentSemesterHolder.clearCache();
        log.info("[SemesterManagement] Deactivated all semesters");
    }

    private SemesterResponse mapToResponse(Semester s) {
        return SemesterResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
