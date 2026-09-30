package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.session.dto.*;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import com.frozenheart.backend.modules.session.service.ExcelParserService;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserPushSettingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseClassManagementServiceImpl implements CourseClassManagementService {

    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final CourseClassRepository courseClassRepository;
    private final UserCourseClassRepository userCourseClassRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final UserPushSettingRepository userPushSettingRepository;
    private final CurrentSemesterHolder currentSemesterHolder;
    private final ExcelParserService excelParserService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ==========================================
    // 1. Quản lý môn học (Subject)
    // ==========================================

    @Override
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        if (subjectRepository.findByCode(cleanCode).isPresent()) {
            throw new AppException(ResponseCode.SUBJECT_ALREADY_EXISTS, "Môn học có mã '" + cleanCode + "' đã tồn tại trong hệ thống");
        }

        Subject subject = Subject.builder()
                .name(request.name().trim())
                .code(cleanCode)
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("[CourseClassManagement] Admin tạo môn học mới: id={}, code={}, name={}", saved.getId(), saved.getCode(), saved.getName());

        return SubjectResponse.builder()
                .subjectId(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(s -> SubjectResponse.builder()
                        .subjectId(s.getId())
                        .code(s.getCode())
                        .name(s.getName())
                        .build())
                .toList();
    }

    // ==========================================
    // 2. Quản lý lớp học phần (CourseClass)
    // ==========================================

    @Override
    @Transactional
    public CourseClassResponse createCourseClass(CreateCourseClassRequest request) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();

        Long targetLecturerId = (request.lecturerId() != null) ? request.lecturerId() : currentUserId;
        User lecturer = userRepository.findById(targetLecturerId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND, "Không tìm thấy giảng viên"));

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException(ResponseCode.SUBJECT_NOT_FOUND, "Không tìm thấy môn học"));

        Semester semester;
        if (request.semesterId() != null) {
            semester = semesterRepository.findById(request.semesterId())
                    .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy học kỳ"));
        } else {
            semester = currentSemesterHolder.getCurrentSemester();
            if (semester == null) {
                throw new AppException(ResponseCode.NO_ACTIVE_SEMESTER, "Không có học kỳ nào đang mở trong hệ thống");
            }
        }

        String cleanClassCode = request.classCode().trim();
        if (courseClassRepository.existsByClassCodeAndSemesterId(cleanClassCode, semester.getId())) {
            throw new AppException(ResponseCode.COURSE_CLASS_ALREADY_EXISTS,
                    "Lớp học phần " + cleanClassCode + " đã tồn tại trong học kỳ " + semester.getName());
        }

        CourseClass courseClass = CourseClass.builder()
                .classCode(cleanClassCode)
                .subject(subject)
                .semester(semester)
                .lecturer(lecturer)
                .createdAt(Instant.now())
                .build();

        CourseClass saved = courseClassRepository.save(courseClass);
        log.info("[CourseClassManagement] Tạo lớp học phần id={}, code={}, subject={}, semester={}, lecturer={}",
                saved.getId(), saved.getClassCode(), subject.getCode(), semester.getName(), lecturer.getId());

        String lecturerName = userProfileRepository.findById(lecturer.getId())
                .map(UserProfile::getFullName)
                .orElse("");

        return CourseClassResponse.builder()
                .courseClassId(saved.getId())
                .classCode(saved.getClassCode())
                .subjectId(subject.getId())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .lecturerId(lecturer.getId())
                .lecturerName(lecturerName)
                .totalStudents(0)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseClassResponse> getCourseClassesByActiveSemester() {
        Semester activeSemester = currentSemesterHolder.getCurrentSemester();
        if (activeSemester == null) {
            return Collections.emptyList();
        }

        List<CourseClass> classes = courseClassRepository.findBySemesterIdFetchAll(activeSemester.getId());
        return mapToCourseClassResponses(classes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseClassResponse> getMyCourseClasses() {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        Semester activeSemester = currentSemesterHolder.getCurrentSemester();
        if (activeSemester == null) {
            return Collections.emptyList();
        }

        List<CourseClass> classes = courseClassRepository.findByLecturerIdAndSemesterIdFetchAll(currentUserId, activeSemester.getId());
        return mapToCourseClassResponses(classes);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseClassResponse getCourseClassDetail(Long courseClassId) {
        CourseClass cc = courseClassRepository.findByIdFetchAll(courseClassId)
                .orElseThrow(() -> new AppException(ResponseCode.COURSE_CLASS_NOT_FOUND, "Không tìm thấy lớp học phần"));
        return mapToCourseClassResponse(cc);
    }

    // ==========================================
    // 3. Gán sinh viên & danh sách lớp
    // ==========================================

    @Override
    @Transactional
    public AssignStudentsResponse assignStudentsToClass(Long courseClassId, AssignStudentsRequest request) {
        CourseClass courseClass = courseClassRepository.findByIdFetchAll(courseClassId)
                .orElseThrow(() -> new AppException(ResponseCode.COURSE_CLASS_NOT_FOUND, "Không tìm thấy lớp học phần"));

        // Kiểm tra phân quyền: Nếu là Giảng viên thì chỉ được thao tác trên lớp mình phụ trách
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        boolean isLecturer = "LECTURER".equalsIgnoreCase(payload.getRole());
        if (isLecturer && courseClass.getLecturer() != null && !courseClass.getLecturer().getId().equals(currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không phụ trách lớp học phần này");
        }

        Set<Long> targetUserIds = new HashSet<>();
        List<String> notFoundCodes = new ArrayList<>();

        // 1. Chống N+1: Tìm theo danh sách MSSV (1 query duy nhất)
        if (request.studentCodes() != null && !request.studentCodes().isEmpty()) {
            Set<String> cleanCodes = request.studentCodes().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            if (!cleanCodes.isEmpty()) {
                List<UserProfile> foundProfiles = userProfileRepository.findAllByStudentLecturerCodeIn(cleanCodes);
                Set<String> foundCodes = new HashSet<>();
                for (UserProfile up : foundProfiles) {
                    targetUserIds.add(up.getUserId());
                    foundCodes.add(up.getStudentLecturerCode());
                }
                for (String code : cleanCodes) {
                    if (!foundCodes.contains(code)) {
                        notFoundCodes.add(code);
                    }
                }
            }
        }

        // 2. Thêm theo danh sách userIds trực tiếp nếu có
        if (request.studentUserIds() != null && !request.studentUserIds().isEmpty()) {
            targetUserIds.addAll(request.studentUserIds());
        }

        if (targetUserIds.isEmpty()) {
            return AssignStudentsResponse.builder()
                    .totalEnrolled(0)
                    .alreadyEnrolledCount(0)
                    .notFoundCount(notFoundCodes.size())
                    .notFoundCodes(notFoundCodes)
                    .build();
        }

        // 3. Chống N+1: Kiểm tra danh sách đã enroll trước đó (1 query duy nhất)
        Set<Long> alreadyEnrolledUserIds = userCourseClassRepository
                .findByIdCourseClassIdAndIdUserIdIn(courseClassId, targetUserIds)
                .stream()
                .map(ucc -> ucc.getUser().getId())
                .collect(Collectors.toSet());

        List<UserCourseClass> enrollmentsToSave = new ArrayList<>();
        Instant now = Instant.now();

        for (Long uid : targetUserIds) {
            if (!alreadyEnrolledUserIds.contains(uid)) {
                User userRef = userRepository.getReferenceById(uid);
                UserCourseClass ucc = UserCourseClass.builder()
                        .id(new UserCourseClassId(uid, courseClassId))
                        .user(userRef)
                        .courseClass(courseClass)
                        .enrolledAt(now)
                        .build();
                enrollmentsToSave.add(ucc);
            }
        }

        // 4. Batch save tất cả sinh viên mới vào lớp
        if (!enrollmentsToSave.isEmpty()) {
            userCourseClassRepository.saveAll(enrollmentsToSave);
        }

        log.info("[CourseClassManagement] Gán sinh viên vào lớp id={}: mới={}, đã có={}, không tìm thấy={}",
                courseClassId, enrollmentsToSave.size(), alreadyEnrolledUserIds.size(), notFoundCodes.size());

        return AssignStudentsResponse.builder()
                .totalEnrolled(enrollmentsToSave.size())
                .alreadyEnrolledCount(alreadyEnrolledUserIds.size())
                .notFoundCount(notFoundCodes.size())
                .notFoundCodes(notFoundCodes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassStudentResponse> getClassStudents(Long courseClassId) {
        // Chống N+1: JOIN FETCH ucc.user u (1 query)
        List<UserCourseClass> enrollments = userCourseClassRepository.findByCourseClassIdFetchUser(courseClassId);
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentUserIds = enrollments.stream()
                .map(ucc -> ucc.getUser().getId())
                .toList();

        // Chống N+1: Batch load toàn bộ profiles của sinh viên trong lớp (1 query)
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(studentUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (p1, _) -> p1));

        return enrollments.stream().map(ucc -> {
            User u = ucc.getUser();
            UserProfile p = profileMap.get(u.getId());
            return ClassStudentResponse.builder()
                    .userId(u.getId())
                    .studentCode(p != null ? p.getStudentLecturerCode() : "")
                    .fullName(p != null ? p.getFullName() : "")
                    .email(u.getEmail())
                    .className(p != null ? p.getClassName() : "")
                    .schoolFaculty(p != null ? p.getSchoolFaculty() : "")
                    .major(p != null ? p.getMajor() : "")
                    .dateOfBirth(p != null ? p.getDateOfBirth() : null)
                    .enrolledAt(ucc.getEnrolledAt())
                    .build();
        }).toList();
    }

    // ==========================================
    // 4. Import 2 giai đoạn từ file Excel
    // ==========================================

    @Override
    @Transactional
    public ExcelImportClassResult importAndEnrollFromExcel(MultipartFile file, Long lecturerId) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String currentRole = payload.getRole();

        // 1. Phân tích cú pháp file Excel
        ExcelParsedClassData parsedData = excelParserService.parseCourseClassExcel(file);
        List<ExcelStudentRow> studentRows = parsedData.students();

        if (studentRows == null || studentRows.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "File Excel không có dữ liệu sinh viên");
        }

        if (parsedData.classCode() == null || parsedData.classCode().isBlank()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy Mã lớp học phần trong file Excel");
        }

        if (parsedData.subjectCode() == null || parsedData.subjectCode().isBlank()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy Mã học phần trong file Excel");
        }

        // =========================================================================
        // GIAI ĐOẠN 1: BULK IMPORT USERS (Tạo tài khoản sinh viên mới nếu chưa có)
        // =========================================================================

        Set<String> fileEmails = studentRows.stream()
                .map(r -> r.email().trim().toLowerCase())
                .filter(e -> !e.isBlank())
                .collect(Collectors.toSet());

        Set<String> fileMssvs = studentRows.stream()
                .map(r -> r.studentCode().trim())
                .filter(m -> !m.isBlank())
                .collect(Collectors.toSet());

        // Chống N+1: Query 1 câu kiểm tra các email đã có trong hệ thống
        Set<String> existingEmails = userRepository.findExistingEmailsByEmailIn(fileEmails).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Chống N+1: Query 1 câu kiểm tra các MSSV đã có trong hệ thống
        Map<String, UserProfile> existingProfilesByMssv = userProfileRepository.findAllByStudentLecturerCodeIn(fileMssvs)
                .stream()
                .collect(Collectors.toMap(UserProfile::getStudentLecturerCode, p -> p, (p1, _) -> p1));

        List<User> usersToSave = new ArrayList<>();
        List<ExcelStudentRow> rowsToSaveProfiles = new ArrayList<>();
        List<User> allStudentUsers = new ArrayList<>();
        Instant now = Instant.now();

        Set<String> processedEmailsInBatch = new HashSet<>();

        for (ExcelStudentRow row : studentRows) {
            String emailLower = row.email().trim().toLowerCase();
            String mssv = row.studentCode().trim();

            boolean existsByEmail = existingEmails.contains(emailLower);
            UserProfile existingProfile = existingProfilesByMssv.get(mssv);

            if (existsByEmail || existingProfile != null) {
                // Người dùng đã tồn tại
                if (existingProfile != null) {
                    allStudentUsers.add(existingProfile.getUser());
                } else {
                    userRepository.findByEmail(emailLower).ifPresent(allStudentUsers::add);
                }
                continue;
            }

            // Tránh trùng lặp trong chính file import
            if (!processedEmailsInBatch.add(emailLower)) {
                continue;
            }

            // Tạo mật khẩu mã hóa BCrypt từ ngày tháng năm sinh viết liền (ddMMyyyy)
            String rawPassword = (row.defaultPassword() != null && !row.defaultPassword().isBlank())
                    ? row.defaultPassword()
                    : mssv;

            User newUser = User.builder()
                    .email(row.email().trim())
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(UserRole.STUDENT)
                    .verified(true)
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            usersToSave.add(newUser);
            rowsToSaveProfiles.add(row);
        }

        int newUsersCreated = 0;
        if (!usersToSave.isEmpty()) {
            List<User> savedUsers = userRepository.saveAll(usersToSave);
            newUsersCreated = savedUsers.size();
            allStudentUsers.addAll(savedUsers);

            List<UserProfile> profilesToSave = new ArrayList<>(savedUsers.size());
            List<UserGamification> gamificationsToSave = new ArrayList<>(savedUsers.size());
            List<UserPushSetting> pushSettingsToSave = new ArrayList<>(savedUsers.size());
            List<Long> savedUserIds = new ArrayList<>(savedUsers.size());

            String schoolFacultyVal = parsedData.department() != null ? parsedData.department() : "";

            for (int i = 0; i < savedUsers.size(); i++) {
                User savedUser = savedUsers.get(i);
                ExcelStudentRow row = rowsToSaveProfiles.get(i);
                savedUserIds.add(savedUser.getId());

                UserProfile profile = UserProfile.builder()
                        .user(savedUser)
                        .fullName(row.fullName())
                        .studentLecturerCode(row.studentCode())
                        .schoolFaculty(schoolFacultyVal)
                        .className(row.className())
                        .major(row.major())
                        .gender(row.gender())
                        .dateOfBirth(row.dateOfBirth())
                        .timezone(Time.DEFAULT_TIMEZONE)
                        .avatarUrl("")
                        .coverUrl("")
                        .avatarFrameUrl("")
                        .bio("Nơi nào có sự sống, nơi đó có công lý!")
                        .totalProposedQuestion(0)
                        .totalApprovedQuestions(0)
                        .badgesCount(0)
                        .friendsCount(0)
                        .followersCount(0)
                        .followingCount(0)
                        .profileCompleted(true)
                        .build();
                profilesToSave.add(profile);

                UserGamification gamification = UserGamification.builder()
                        .user(savedUser)
                        .publicPoints(0.0)
                        .secretPoints(0.0)
                        .coinBalance(0.0)
                        .currentStreak(0)
                        .build();
                gamificationsToSave.add(gamification);

                UserPushSetting pushSetting = UserPushSetting.builder()
                        .user(savedUser)
                        .preferences(PushPreferences.createDefault())
                        .updatedAt(now)
                        .build();
                pushSettingsToSave.add(pushSetting);
            }

            userProfileRepository.saveAll(profilesToSave);
            userGamificationRepository.saveAll(gamificationsToSave);
            userPushSettingRepository.saveAll(pushSettingsToSave);

            // Bắn event đồng bộ Elasticsearch
            eventPublisher.publishEvent(EntitySearchSyncEvent.upsertBatch(EntitySearchSyncEvent.EntityType.USER, savedUserIds));
        }

        // =========================================================================
        // GIAI ĐOẠN 2: LỚP HỌC PHẦN & GÁN SINH VIÊN (Class Enrollment)
        // =========================================================================

        // 1. Học kỳ: Lấy từ CurrentSemesterHolder (Không tạo mới)
        Semester semester = currentSemesterHolder.getCurrentSemester();
        if (semester == null) {
            throw new AppException(ResponseCode.NO_ACTIVE_SEMESTER, "Không có học kỳ nào đang mở trong hệ thống");
        }

        // 2. Môn học: Chỉ tìm kiếm theo mã môn học (Không tạo mới)
        String cleanSubjectCode = parsedData.subjectCode().trim().toUpperCase();
        Subject subject = subjectRepository.findByCode(cleanSubjectCode)
                .orElseThrow(() -> new AppException(ResponseCode.SUBJECT_NOT_FOUND,
                        "Không tìm thấy môn học có mã '" + cleanSubjectCode + "' trong hệ thống. Vui lòng liên hệ Quản trị viên để tạo môn học trước khi import!"));

        // 3. Lớp học phần: Tìm theo mã lớp và học kỳ; nếu chưa có thì tự tạo
        String cleanClassCode = parsedData.classCode().trim();
        CourseClass courseClass = courseClassRepository.findByClassCodeAndSemesterId(cleanClassCode, semester.getId())
                .orElseGet(() -> {
                    User assignedLecturer = resolveLecturerForClass(currentUserId, currentRole, parsedData.lecturerName(), lecturerId);
                    CourseClass newClass = CourseClass.builder()
                            .classCode(cleanClassCode)
                            .subject(subject)
                            .semester(semester)
                            .lecturer(assignedLecturer)
                            .createdAt(now)
                            .build();
                    CourseClass savedClass = courseClassRepository.save(newClass);
                    log.info("[CourseClassManagement] Tự động tạo lớp học phần từ Excel: id={}, code={}, semester={}, lecturerId={}",
                            savedClass.getId(), cleanClassCode, semester.getName(), assignedLecturer.getId());
                    return savedClass;
                });

        // 4. Chống N+1: Lấy danh sách ID của tất cả sinh viên
        Set<Long> allUserIds = allStudentUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // Chống N+1: Query 1 câu kiểm tra các sinh viên đã được gán vào lớp này từ trước
        Set<Long> alreadyEnrolledIds = userCourseClassRepository
                .findByIdCourseClassIdAndIdUserIdIn(courseClass.getId(), allUserIds)
                .stream()
                .map(ucc -> ucc.getUser().getId())
                .collect(Collectors.toSet());

        List<UserCourseClass> newEnrollments = new ArrayList<>();
        for (Long uid : allUserIds) {
            if (!alreadyEnrolledIds.contains(uid)) {
                User uRef = userRepository.getReferenceById(uid);
                UserCourseClass ucc = UserCourseClass.builder()
                        .id(new UserCourseClassId(uid, courseClass.getId()))
                        .user(uRef)
                        .courseClass(courseClass)
                        .enrolledAt(now)
                        .build();
                newEnrollments.add(ucc);
            }
        }

        // 5. Batch save toàn bộ sinh viên mới vào lớp
        if (!newEnrollments.isEmpty()) {
            userCourseClassRepository.saveAll(newEnrollments);
        }

        int existingUsersFound = allStudentUsers.size() - newUsersCreated;
        log.info("[CourseClassManagement] Hoàn tất import Excel lớp {}: tổng={}, user mới={}, user cũ={}, enroll mới={}, đã enroll={}",
                cleanClassCode, studentRows.size(), newUsersCreated, existingUsersFound, newEnrollments.size(), alreadyEnrolledIds.size());

        return ExcelImportClassResult.builder()
                .courseClassId(courseClass.getId())
                .classCode(courseClass.getClassCode())
                .subjectCode(subject.getCode())
                .subjectName(subject.getName())
                .semesterName(semester.getName())
                .totalRowsInFile(studentRows.size())
                .newUsersCreated(newUsersCreated)
                .existingUsersFound(existingUsersFound)
                .newEnrollments(newEnrollments.size())
                .alreadyEnrolledCount(alreadyEnrolledIds.size())
                .build();
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private List<CourseClassResponse> mapToCourseClassResponses(List<CourseClass> classes) {
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }

        // Chống N+1: Batch load tên giảng viên của tất cả các lớp trong 1 query duy nhất
        Set<Long> lecturerIds = classes.stream()
                .map(cc -> cc.getLecturer() != null ? cc.getLecturer().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> lecturerNameMap = lecturerIds.isEmpty()
                ? Collections.emptyMap()
                : userProfileRepository.findAllById(lecturerIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName, (p1, _) -> p1));

        return classes.stream().map(cc -> {
            Subject s = cc.getSubject();
            Semester sem = cc.getSemester();
            User lecturer = cc.getLecturer();
            String lecturerName = (lecturer != null) ? lecturerNameMap.getOrDefault(lecturer.getId(), "") : "";

            return CourseClassResponse.builder()
                    .courseClassId(cc.getId())
                    .classCode(cc.getClassCode())
                    .subjectId(s != null ? s.getId() : null)
                    .subjectCode(s != null ? s.getCode() : null)
                    .subjectName(s != null ? s.getName() : null)
                    .semesterId(sem != null ? sem.getId() : null)
                    .semesterName(sem != null ? sem.getName() : null)
                    .lecturerId(lecturer != null ? lecturer.getId() : null)
                    .lecturerName(lecturerName)
                    .totalStudents(cc.getStudentEnrollments() != null ? cc.getStudentEnrollments().size() : 0)
                    .createdAt(cc.getCreatedAt())
                    .build();
        }).toList();
    }

    private CourseClassResponse mapToCourseClassResponse(CourseClass cc) {
        Subject s = cc.getSubject();
        Semester sem = cc.getSemester();
        User lecturer = cc.getLecturer();
        String lecturerName = (lecturer != null)
                ? userProfileRepository.findById(lecturer.getId()).map(UserProfile::getFullName).orElse("")
                : "";

        return CourseClassResponse.builder()
                .courseClassId(cc.getId())
                .classCode(cc.getClassCode())
                .subjectId(s != null ? s.getId() : null)
                .subjectCode(s != null ? s.getCode() : null)
                .subjectName(s != null ? s.getName() : null)
                .semesterId(sem != null ? sem.getId() : null)
                .semesterName(sem != null ? sem.getName() : null)
                .lecturerId(lecturer != null ? lecturer.getId() : null)
                .lecturerName(lecturerName)
                .totalStudents(cc.getStudentEnrollments() != null ? cc.getStudentEnrollments().size() : 0)
                .createdAt(cc.getCreatedAt())
                .build();
    }

    private User resolveLecturerForClass(Long currentUserId, String currentRole, String lecturerNameInFile, Long specificLecturerId) {
        // 1. Nếu có lecturerId được chỉ định rõ từ request param (Admin chọn từ dropdown trên giao diện)
        if (specificLecturerId != null) {
            User specificLecturer = userRepository.findById(specificLecturerId)
                    .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND,
                            "Không tìm thấy giảng viên được chỉ định với ID: " + specificLecturerId));
            if (specificLecturer.getRole() != UserRole.LECTURER && specificLecturer.getRole() != UserRole.ADMIN) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                        "Người dùng được chỉ định không phải là Giảng viên hoặc Quản trị viên");
            }
            return specificLecturer;
        }

        // 2. Nếu người thực hiện là LECTURER (Giảng viên tự upload file lớp của mình)
        boolean isLecturer = "LECTURER".equalsIgnoreCase(currentRole) || "ROLE_LECTURER".equalsIgnoreCase(currentRole);
        if (isLecturer) {
            return userRepository.getReferenceById(currentUserId);
        }

        // 3. Nếu người thực hiện là ADMIN: Tự động tìm kiếm Giảng viên theo tên từ cột GV giảng dạy trong file Excel
        if (lecturerNameInFile != null && !lecturerNameInFile.isBlank()) {
            List<UserProfile> matchingProfiles = userProfileRepository.findActiveLecturersByFullName(lecturerNameInFile.trim());
            if (matchingProfiles.size() == 1) {
                User matched = matchingProfiles.get(0).getUser();
                log.info("[CourseClassManagement] Admin import: Tự động gán lớp cho giảng viên '{}' (ID: {}) tìm thấy từ file Excel",
                        lecturerNameInFile, matched.getId());
                return matched;
            } else if (matchingProfiles.size() > 1) {
                User firstMatched = matchingProfiles.get(0).getUser();
                log.warn("[CourseClassManagement] Admin import: Có {} giảng viên trùng tên '{}'. Ưu tiên chọn giảng viên đầu tiên (ID: {}). Khuyến nghị chỉ định lecturerId cụ thể.",
                        matchingProfiles.size(), lecturerNameInFile, firstMatched.getId());
                return firstMatched;
            }
        }

        // 4. Fallback khi không tìm thấy giảng viên:
        String nameHint = (lecturerNameInFile != null && !lecturerNameInFile.isBlank()) ? " '" + lecturerNameInFile + "'" : "";
        throw new AppException(ResponseCode.USER_NOT_FOUND,
                "Không tìm thấy tài khoản Giảng viên" + nameHint + " trong hệ thống. Vui lòng tạo tài khoản cho Giảng viên trước hoặc chọn Giảng viên phụ trách khi import!");
    }
}
