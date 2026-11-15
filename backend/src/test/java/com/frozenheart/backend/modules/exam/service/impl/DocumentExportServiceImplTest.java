package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.ExamQuestion;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceImplTest {

    private static final long USER_ID = 7L;
    private static final long SUBJECT_ID = 12L;
    private static final String EXAM_TITLE = "Đề thi cuối kỳ môn Công nghệ Web và dịch vụ trực tuyến";

    @Mock private MinioClient minioClient;
    @Mock private QuestionRepository questionRepository;
    @Mock private CourseClassRepository courseClassRepository;
    @Mock private MediaService mediaService;
    @TempDir private Path mediaDirectory;

    private R2Properties properties;
    private DocumentExportServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new R2Properties();
        properties.setBucketName("sqb-public");
        properties.setReportEvidenceBucket("sqb-secret");
        service = new DocumentExportServiceImpl(minioClient, properties, questionRepository, courseClassRepository, mediaService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"LECTURER", "STUDENT"})
    void deniesExportBeforeReadingQuestionsOrSigningAUrl(UserRole role) {
        authenticate(role);

        AppException error = assertThrows(AppException.class, () -> service.exportQuestions(SUBJECT_ID, "pdf", true));

        assertEquals(ResponseCode.ACCESS_DENIED, error.getResponseCode());
        if (role == UserRole.LECTURER) {
            verify(courseClassRepository).existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(USER_ID, SUBJECT_ID);
        } else {
            verifyNoInteractions(courseClassRepository);
        }
        verifyNoInteractions(questionRepository, minioClient, mediaService);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "LECTURER"})
    void exportsQuestionsOfAllStatusesForAnAuthorizedSubject(UserRole role) throws Exception {
        authenticate(role);
        if (role == UserRole.LECTURER) {
            when(courseClassRepository.existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(USER_ID, SUBJECT_ID))
                    .thenReturn(true);
        }
        Question approved = questionWithImages();
        approved.setStatus(QuestionStatus.APPROVED);
        when(questionRepository.findAllBySubjectIdForExport(SUBJECT_ID)).thenReturn(List.of(
                approved,
                Question.builder().content("Câu chờ duyệt").status(QuestionStatus.PENDING).build(),
                Question.builder().content("Câu bị từ chối").status(QuestionStatus.REJECTED).build()));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.test/study-material.xlsx");

        var file = captureUpload();
        var result = service.exportQuestions(SUBJECT_ID, "excel", true);

        assertEquals(3, result.getQuestionsCount());
        assertEquals("https://example.test/study-material.xlsx", result.getDownloadUrl());
        verifyNewPrivateExport("exports/questions/v2/subject_12_ans_true.xlsx", "tai-lieu-on-tap-12.xlsx");
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.get()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Tài liệu ôn tập", sheet.getRow(0).getCell(0).getStringCellValue());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals(approved.getContent(), sheet.getRow(4).getCell(1).getStringCellValue());
            assertTrue(sheet.getRow(4).getCell(2).getStringCellValue().contains("(Đúng)"));
            assertEquals(approved.getExplanation(), sheet.getRow(4).getCell(3).getStringCellValue());
            assertEquals("Câu chờ duyệt", sheet.getRow(5).getCell(1).getStringCellValue());
            assertEquals("Câu bị từ chối", sheet.getRow(6).getCell(1).getStringCellValue());
            assertEquals(CellType.BLANK, sheet.getRow(5).getCell(3).getCellType());
        }
        if (role == UserRole.ADMIN) {
            verifyNoInteractions(courseClassRepository);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersSortedExamWorkbookAndSignsItInPrivateBucket(boolean withAnswer) throws Exception {
        var file = captureUpload();
        service.exportExam(unsortedExam(questionWithImages()), "xlsx", withAnswer, "A4");

        verifyNewPrivateExport("exports/exams/v2/exam_19_ans_" + withAnswer + ".xlsx", "de-thi-19.xlsx");
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.get()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(EXAM_TITLE, sheet.getRow(0).getCell(0).getStringCellValue());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals(withAnswer ? 4 : 3, sheet.getRow(3).getLastCellNum());
            assertEquals(1.0, sheet.getRow(4).getCell(0).getNumericCellValue());
            assertEquals(2.0, sheet.getRow(5).getCell(0).getNumericCellValue());
            if (withAnswer) {
                assertEquals("A", sheet.getRow(4).getCell(3).getStringCellValue());
            }
        }
    }

    @Test
    void signsCachedPdfInPrivateBucketWithoutUploading() throws Exception {
        var cached = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(cached);

        service.exportExam(Exam.builder().id(19L).build(), "pdf", true, "A4");

        String key = "exports/exams/v2/exam_19_ans_true.pdf";
        verifyPrivateLookupAndLink(key);
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        verifyNoInteractions(mediaService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersExamPdfWithVietnameseTextAndImages(boolean withAnswer) throws Exception {
        var pdf = captureUpload();

        service.exportExam(unsortedExam(questionWithImages()), "pdf", withAnswer, "A4");

        verifyNewPrivateExport("exports/exams/v2/exam_19_ans_" + withAnswer + ".pdf", "de-thi-19.pdf");
        String text = readPdfWithImages(pdf.get(), 1);
        assertTrue(text.contains("Đề thi cuối kỳ môn Công nghệ Web"), text);
        assertTrue(text.contains("Câu 1") && text.contains("Câu 2") && text.indexOf("Câu 1") < text.indexOf("Câu 2"), text);
        assertEquals(withAnswer, text.contains("Đáp án đúng: A"), text);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersStudyPdfWithVietnameseTextAndImages(boolean withAnswer) throws Exception {
        authenticate(UserRole.LECTURER);
        when(courseClassRepository.existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(USER_ID, SUBJECT_ID))
                .thenReturn(true);
        when(questionRepository.findAllBySubjectIdForExport(SUBJECT_ID))
                .thenReturn(Collections.nCopies(5, questionWithImages()));
        var pdf = captureUpload();

        service.exportQuestions(SUBJECT_ID, "pdf", withAnswer);

        verifyNewPrivateExport("exports/questions/v2/subject_12_ans_" + withAnswer + ".pdf", "tai-lieu-on-tap-12.pdf");
        String text = readPdfWithImages(pdf.get(), 2);
        assertTrue(text.contains("Tài liệu ôn tập"), text);
        assertTrue(text.contains("Câu 5"), text);
        assertEquals(withAnswer, text.contains("Lời giải: Trình duyệt đọc phản hồi"), text);
        assertEquals(withAnswer, text.contains("[Đáp án đúng]"), text);
    }

    private Exam unsortedExam(Question first) {
        var second = Question.builder()
                .content("Cơ chế ghép kênh cho phép trình duyệt thực hiện nhiều yêu cầu và nhận nhiều phản hồi trên cùng một kết nối TCP. Cơ chế này mang lại lợi ích gì so với HTTP/1.1?")
                .options(first.getOptions()).build();
        return Exam.builder().id(19L).title(EXAM_TITLE)
                .subject(first.getSession().getCourseClass().getSubject())
                .examQuestion(ExamQuestion.builder().displayOrder(2).question(second).build())
                .examQuestion(ExamQuestion.builder().displayOrder(1).question(first)
                        .optionsSnapshot(first.getOptions()).build())
                .build();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "sqb-public"})
    void refusesBothExportsWithoutADistinctPrivateBucket(String bucket) {
        properties.setReportEvidenceBucket(bucket);
        authenticate(UserRole.ADMIN);

        assertEquals(ResponseCode.FILE_UPLOAD_FAILED,
                assertThrows(AppException.class, () -> service.exportQuestions(SUBJECT_ID, "pdf", true)).getResponseCode());
        assertEquals(ResponseCode.FILE_UPLOAD_FAILED,
                assertThrows(AppException.class,
                        () -> service.exportExam(Exam.builder().id(19L).build(), "pdf", true, "A4")).getResponseCode());
        verifyNoInteractions(courseClassRepository, questionRepository, minioClient, mediaService);
    }

    private Question questionWithImages() throws Exception {
        var option = new QuestionOption();
        option.setKey("A");
        option.setText("Phương án sử dụng HTTPS");
        option.setIsCorrect(true);
        option.setMediaUrl(createImage("option.png", Color.BLUE));
        var otherOption = new QuestionOption();
        otherOption.setKey("B");
        otherOption.setText("Phương án không có ảnh");
        otherOption.setIsCorrect(false);
        return Question.builder().content("Giao thức nào mã hóa dữ liệu khi truyền qua mạng?")
                .explanation("Trình duyệt đọc phản hồi")
                .session(Session.builder().courseClass(CourseClass.builder()
                        .subject(Subject.builder().name("Công nghệ Web và dịch vụ trực tuyến").build()).build()).build())
                .ownedMedia(QuestionMedia.builder().mediaTarget(MediaTarget.CONTENT)
                        .url(createImage("question.png", Color.ORANGE)).build())
                .options(List.of(option, otherOption)).build();
    }

    private String createImage(String name, Color color) throws Exception {
        var image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        Path path = mediaDirectory.resolve(name);
        ImageIO.write(image, "png", path.toFile());
        return path.toUri().toString();
    }

    private AtomicReference<byte[]> captureUpload() throws Exception {
        var pdf = new AtomicReference<byte[]>();
        when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(invocation -> {
            PutObjectArgs args = invocation.getArgument(0);
            pdf.set(args.stream().readAllBytes());
            return null;
        });
        return pdf;
    }

    private String readPdfWithImages(byte[] pdf, int minimumPages) throws Exception {
        var reader = new PdfReader(pdf);
        try {
            assertTrue(reader.getNumberOfPages() >= minimumPages);
            var images = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT);
            assertEquals(2, images.size());
            var extractor = new PdfTextExtractor(reader);
            var pageText = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                pageText.append(extractor.getTextFromPage(page)).append('\n');
            }
            var text = pageText.toString();
            assertTrue(text.contains("Giao thức nào mã hóa dữ liệu khi truyền qua mạng?"), text);
            assertTrue(text.contains("A. Phương án sử dụng HTTPS"), text);
            assertTrue(text.contains("B. Phương án không có ảnh"), text);
            return text;
        } finally {
            reader.close();
        }
    }

    private void verifyNewPrivateExport(String key, String fileName) throws Exception {
        verifyPrivateLookupAndLink(key);
        verify(minioClient).putObject(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)
                && args.headers().entries().stream().anyMatch(header ->
                        header.getKey().equalsIgnoreCase("Content-Disposition")
                                && header.getValue().equals("attachment; filename=\"" + fileName + "\""))));
        verify(mediaService).confirmMediaPermanent(List.of(key));
    }

    private void verifyPrivateLookupAndLink(String key) throws Exception {
        verify(minioClient).statObject(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)));
        verify(minioClient).getPresignedObjectUrl(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)));
    }

    private void authenticate(UserRole role) {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(USER_ID).role(role.name()).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
