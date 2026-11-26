package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentSemesterService {

    private final SemesterRepository semesterRepository;

    public Semester findCurrentSemester() {
        return semesterRepository.findByActiveTrue().orElse(null);
    }

    public Semester requireCurrentSemester() {
        return semesterRepository.findByActiveTrue().orElseThrow(() ->
                new AppException(ResponseCode.NO_ACTIVE_SEMESTER, "Không có học kỳ nào đang hoạt động."));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Semester lockSemesterForPoints(Semester semester) {
        if (semester == null) {
            return null;
        }
        // Check under the database lock even when the caller has loaded an older Semester entity.
        return semesterRepository.lockUnfinalizedSemesterId(semester.getId()).isPresent() ? semester : null;
    }
}
