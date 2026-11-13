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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterManagementServiceImpl implements SemesterManagementService {

    private static final String SINGLE_ACTIVE_INDEX = "idx_semesters_single_active";

    private final SemesterRepository semesterRepository;
    private final CourseClassRepository courseClassRepository;

    @Override
    @Transactional
    public SemesterResponse createSemester(CreateSemesterRequest request) {
        Semester semester = Semester.builder()
                .name(request.name().trim())
                .startDate(request.startDate())
                .active(false)
                .isFinalized(false)
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
    public SemesterResponse updateSemesterStartDate(Long semesterId, LocalDate startDate) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy học kỳ"));
        semester.setStartDate(startDate);
        return mapToResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse activateSemester(Long semesterId) {
        // Kiểm tra an toàn: Đảm bảo không có học kỳ nào đang hoạt động
        Optional<Semester> currentActiveOpt = semesterRepository.findByActiveTrue();
        if (currentActiveOpt.isPresent()) {
            Semester currentActive = currentActiveOpt.get();
            // Nếu học kỳ Admin muốn kích hoạt chính là học kỳ đang mở thì bỏ qua
            if (currentActive.getId().equals(semesterId)) {
                return mapToResponse(currentActive);
            }

            // Nếu có một học kỳ khác đang mở, ném lỗi chặn lại
            throw new AppException(
                ResponseCode.ACTIVE_SEMESTER_CONFLICT,
                String.format("Không thể kích hoạt! Học kỳ '%s' đang hoạt động. Vui lòng thực hiện Tổng kết/Chốt sổ và Đóng học kỳ này trước.", currentActive.getName())
            );
        }

        // Lấy học kỳ cần kích hoạt
        Semester semester = semesterRepository.findByIdForUpdate(semesterId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy học kỳ"));

        if (semester.isActive()) {
            return mapToResponse(semester);
        }
        if (semester.isFinalized()) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Học kỳ này đã được chốt sổ, không thể kích hoạt lại.");
        }

        // Kích hoạt học kỳ được chọn
        semester.setActive(true);
        try {
            semesterRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && SINGLE_ACTIVE_INDEX.equals(violation.getConstraintName())) {
                    throw new AppException(ResponseCode.ACTIVE_SEMESTER_CONFLICT,
                            "Một học kỳ khác vừa được kích hoạt. Hãy kiểm tra lại danh sách học kỳ.");
                }
            }
            throw exception;
        }

        log.info("[SemesterManagement] Activated semester id={}, name={}", semester.getId(), semester.getName());
        return mapToResponse(semester);
    }

    @Override
    @Transactional
    public void deactivateAllSemesters() {
        Optional<Semester> activeOpt = semesterRepository.findActiveForUpdate();

        if (activeOpt.isEmpty()) {
            throw new AppException(
                ResponseCode.ACTION_NOT_ALLOWED,
                "Hiện không có học kỳ nào đang hoạt động để hủy kích hoạt."
            );
        }

        Semester activeSemester = activeOpt.get();

        // Kiểm tra cờ tổng kết / chốt sổ
        if (!activeSemester.isFinalized()) {
            throw new AppException(
                ResponseCode.ACTION_NOT_ALLOWED,
                String.format("Không thể đóng học kỳ! Học kỳ '%s' đang hoạt động chưa được chốt sổ. Bạn phải chạy Chốt Sổ Học Kỳ trước.", activeSemester.getName())
            );
        }

        activeSemester.setActive(false);
        log.info("[SemesterManagement] Deactivated all semesters");
    }

    @Override
    @Transactional
    public void deleteSemester(Long semesterId) {
        Semester semester = semesterRepository.findByIdForUpdate(semesterId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy học kỳ để xóa"));

        if (semester.isActive()) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Không thể xóa học kỳ đang hoạt động. Vui lòng đóng học kỳ trước khi xóa.");
        }

        if (courseClassRepository.existsBySemesterId(semesterId)) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Không thể xóa học kỳ đã có lớp học phần trực thuộc.");
        }

        semesterRepository.delete(semester);
        log.info("[SemesterManagement] Deleted semester id={}, name={}", semesterId, semester.getName());
    }

    private SemesterResponse mapToResponse(Semester s) {
        return SemesterResponse.builder()
                .semesterId(s.getId())
                .name(s.getName())
                .startDate(s.getStartDate())
                .active(s.isActive())
                .isFinalize(s.isFinalized())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
