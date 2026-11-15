package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.ExamQuestion;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.ExamQuestionReportDto;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.QuestionReportDto;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.media.service.R2StorageQuota;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExportServiceImpl implements DocumentExportService {

    private static final String EXPORT_VERSION = "v2";
    private static final int EXCEL_HEADER_ROW = 3;

    private final MinioClient minioClient;
    private final R2Properties r2Properties;
    private final QuestionRepository questionRepository;
    private final CourseClassRepository courseClassRepository;
    private final MediaService mediaService;
    private final R2StorageQuota storageQuota;

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportQuestions(Long subjectId, String format, Boolean includeAnswer) {
        JwtPayload user = JwtPayload.getCurrentUserPayload();
        boolean canExport = UserRole.ADMIN.name().equals(user.getRole())
                || (UserRole.LECTURER.name().equals(user.getRole())
                    && courseClassRepository.existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(user.getUserId(), subjectId));
        if (!canExport) {
            throw new AppException(ResponseCode.ACCESS_DENIED,
                    "Bạn chỉ có thể xuất tài liệu ôn tập cho môn được phân công trong học kỳ hiện tại");
        }

        String bucket = privateExportBucket();
        boolean withAnswer = includeAnswer == null || includeAnswer;
        String ext = "excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format) ? "xlsx" : "pdf";

        // Đặt tên chung như thế này để nếu có người cùng tải về 1 loại thì dùng cái này cache luôn
        String objectKey = String.format("exports/questions/%s/subject_%d_ans_%b.%s", EXPORT_VERSION, subjectId, withAnswer, ext);
        String fileName = "tai-lieu-on-tap-" + subjectId + "." + ext;

        // Fetch Questions (JOIN FETCH session, subject, ownedMedias)
        List<Question> questions = questionRepository.findAllBySubjectIdForExport(subjectId);
        if (questions.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Không tìm thấy câu hỏi nào thuộc môn học này");
        }

        // Check MinIO Cache
        ExportResponse cachedResponse = checkMinioCache(bucket, objectKey, questions.size());
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // Render Document Byte Array
        byte[] contentBytes;
        String contentType;
        if ("xlsx".equalsIgnoreCase(ext)) {
            contentBytes = generateQuestionsExcel(questions, withAnswer);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            contentBytes = generateQuestionsPdf(questions, withAnswer);
            contentType = "application/pdf";
        }

        // Upload to MinIO & Generate Presigned URL
        return uploadAndPresign(bucket, objectKey, fileName, contentBytes, contentType, questions.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportExam(Exam exam, String format, Boolean includeAnswerKey, String paperSize) {
        String bucket = privateExportBucket();
        boolean withAnswer = includeAnswerKey != null && includeAnswerKey;
        String ext = "excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format) ? "xlsx" : "pdf";
        String objectKey = String.format("exports/exams/%s/exam_%d_ans_%b.%s", EXPORT_VERSION, exam.getId(), withAnswer, ext);
        String fileName = "de-thi-" + exam.getId() + "." + ext;

        int questionCount = exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0;

        // Check MinIO Cache
        ExportResponse cachedResponse = checkMinioCache(bucket, objectKey, questionCount);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        List<ExamQuestion> questions = exam.getExamQuestions() == null ? List.of() : exam.getExamQuestions().stream()
                .sorted(Comparator.comparingInt(ExamQuestion::getDisplayOrder))
                .toList();

        // Render Document
        byte[] contentBytes;
        String contentType;
        if ("xlsx".equalsIgnoreCase(ext)) {
            contentBytes = generateExamExcel(exam, questions, withAnswer);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            contentBytes = generateExamPdf(exam, questions, withAnswer);
            contentType = "application/pdf";
        }

        // Upload to MinIO & Presign
        return uploadAndPresign(bucket, objectKey, fileName, contentBytes, contentType, questionCount);
    }

    private String privateExportBucket() {
        String bucket = r2Properties.getReportEvidenceBucket();
        if (bucket == null || bucket.isBlank() || bucket.equals(r2Properties.getBucketName())) {
            throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Kho tệp xuất riêng tư chưa được cấu hình.");
        }
        return bucket;
    }

    private ExportResponse checkMinioCache(String bucket, String objectKey, int questionsCount) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            if (stat != null) {
                String downloadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(objectKey)
                                .expiry(Time.DEFAULT_EXPIRATION_SECONDS)
                                .build());
                double fileSizeMb = Math.round((stat.size() / (1024.0 * 1024.0)) * 100.0) / 100.0;
                return ExportResponse.builder()
                        .downloadUrl(downloadUrl)
                        .fileSizeMb(fileSizeMb)
                        .questionsCount(questionsCount)
                        .expiresAt(Instant.now().plusSeconds(Time.DEFAULT_EXPIRATION_SECONDS))
                        .build();
            }
        } catch (Exception e) {
            log.debug("[DocumentExportService] Object not found in MinIO cache: {}", objectKey);
        }
        return null;
    }

    private ExportResponse uploadAndPresign(String bucket, String objectKey, String fileName, byte[] contentBytes,
            String contentType, int questionsCount) {
        try {
            try (InputStream is = new ByteArrayInputStream(contentBytes)) {
                storageQuota.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(is, (long) contentBytes.length, -1L)
                                .contentType(contentType)
                                .headers(Map.of(
                                        "Cache-Control", "private, no-store",
                                        "Content-Disposition", ContentDisposition.attachment().filename(fileName).build().toString()))
                                .build());
            }
            mediaService.confirmMediaPermanent(List.of(objectKey));

            String downloadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(Time.DEFAULT_EXPIRATION_SECONDS)
                            .build());

            double fileSizeMb = Math.round((contentBytes.length / (1024.0 * 1024.0)) * 100.0) / 100.0;
            return ExportResponse.builder()
                    .downloadUrl(downloadUrl)
                    .fileSizeMb(fileSizeMb)
                    .questionsCount(questionsCount)
                    .expiresAt(Instant.now().plusSeconds(Time.DEFAULT_EXPIRATION_SECONDS))
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DocumentExportService] Lỗi khi upload/presign file export: ", e);
            throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Xuất file thất bại: " + e.getMessage());
        }
    }

    // --- Render Methods ---

    private XSSFCellStyle exportCellStyle(XSSFWorkbook workbook, String fill, String text, int size, boolean bold) {
        var font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) size);
        font.setBold(bold);
        font.setColor(new XSSFColor(HexFormat.of().parseHex(text)));

        var style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(HexFormat.of().parseHex(fill)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setIndention((short) 1);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void createWorksheetHeader(XSSFWorkbook workbook, Sheet sheet, String title, String subtitle,
            String accent, String tint, String[] headers, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        var titleStyle = exportCellStyle(workbook, accent, "FFFFFF", 17, true);
        var subtitleStyle = exportCellStyle(workbook, tint, "334155", 11, false);
        var headerStyle = exportCellStyle(workbook, "26394C", "FFFFFF", 11, true);
        int totalWidth = Arrays.stream(widths).sum();
        writeMergedHeading(sheet, 0, title, titleStyle, headers.length, totalWidth, 25);
        writeMergedHeading(sheet, 1, subtitle, subtitleStyle, headers.length, totalWidth, 16);

        Row row = sheet.createRow(EXCEL_HEADER_ROW);
        row.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(1, EXCEL_HEADER_ROW + 1);
        sheet.setRepeatingRows(new CellRangeAddress(EXCEL_HEADER_ROW, EXCEL_HEADER_ROW, -1, -1));
    }

    private void writeMergedHeading(Sheet sheet, int rowIndex, String text, CellStyle style, int columns,
            int width, int pointsPerLine) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(Math.min(409, 10 + wrappedLineCount(text, width) * pointsPerLine));
        for (int i = 0; i < columns; i++) {
            row.createCell(i).setCellStyle(style);
        }
        row.getCell(0).setCellValue(text);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columns - 1));
    }

    private int wrappedLineCount(String text, int width) {
        int charsPerLine = Math.max(1, width * 4 / 5);
        return Arrays.stream(text.split("\\R", -1))
                .mapToInt(line -> Math.max(1, (line.length() + charsPerLine - 1) / charsPerLine))
                .sum();
    }

    private void writeQuestionRow(Sheet sheet, int rowIndex, CellStyle style, int[] widths, int order,
            String... values) {
        Row row = sheet.createRow(rowIndex);
        Cell numberCell = row.createCell(0);
        numberCell.setCellValue(order);
        numberCell.setCellStyle(style);

        int lines = 1;
        for (int i = 0; i < values.length; i++) {
            String value = Objects.toString(values[i], "");
            Cell cell = row.createCell(i + 1);
            cell.setCellStyle(style);
            if (!value.isEmpty()) {
                cell.setCellValue(value);
                lines = Math.max(lines, wrappedLineCount(value, widths[i + 1]));
            }
        }
        row.setHeightInPoints(Math.min(409, 7 + lines * 15));
    }

    private byte[] generateQuestionsExcel(List<Question> questions, boolean includeAnswer) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh Sach Cau Hoi");
            String[] headers = includeAnswer ? new String[] {"STT", "Câu hỏi", "Phương án", "Lời giải"}
                    : new String[] {"STT", "Câu hỏi", "Phương án"};
            int[] widths = includeAnswer ? new int[] {9, 58, 52, 44} : new int[] {9, 64, 58};
            createWorksheetHeader(workbook, sheet, "Tài liệu ôn tập",
                    subjectAndQuestionCount(studySubjectName(questions), questions.size()),
                    "075B56", "E8F5F1", headers, widths);
            var white = exportCellStyle(workbook, "FFFFFF", "1B2938", 11, false);
            var tinted = exportCellStyle(workbook, "F1F8F6", "1B2938", 11, false);

            int order = 1;
            for (Question question : questions) {
                List<QuestionOption> options = question.getOptions();
                String optionText = options == null ? "" : options.stream()
                        .map(option -> option.getKey() + ". " + Objects.toString(option.getText(), "")
                                + (includeAnswer && Boolean.TRUE.equals(option.getIsCorrect()) ? " (Đúng)" : ""))
                        .collect(Collectors.joining("\n"));
                String[] values = includeAnswer
                        ? new String[] {question.getContent(), optionText, question.getExplanation()}
                        : new String[] {question.getContent(), optionText};
                writeQuestionRow(sheet, EXCEL_HEADER_ROW + order, order % 2 == 1 ? white : tinted,
                        widths, order, values);
                order++;
            }
            sheet.setAutoFilter(new CellRangeAddress(EXCEL_HEADER_ROW, sheet.getLastRowNum(), 0, headers.length - 1));
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR, "Tạo Excel thất bại: " + e.getMessage());
        }
    }

    private String studySubjectName(List<Question> questions) {
        if (questions.isEmpty()) {
            return "";
        }
        var session = questions.getFirst().getSession();
        var courseClass = session != null ? session.getCourseClass() : null;
        var subject = courseClass != null ? courseClass.getSubject() : null;
        return subject != null ? subject.getName() : "";
    }

    private String subjectAndQuestionCount(String subject, int count) {
        String subjectLabel = subject == null || subject.isBlank() ? "" : "Môn học: " + subject + "  ·  ";
        return subjectLabel + count + " câu hỏi";
    }

    private record FormattedOptions(String optionsText, String correctAnswers) {
    }

    private FormattedOptions formatExamQuestionOptions(ExamQuestion eq) {
        StringBuilder optsSb = new StringBuilder();
        StringBuilder correctSb = new StringBuilder();
        List<QuestionOption> options = eq.getOptionsSnapshot() != null ? eq.getOptionsSnapshot()
                : eq.getQuestion().getOptions();
        if (options != null) {
            for (QuestionOption opt : options) {
                optsSb.append(opt.getKey()).append(". ").append(opt.getText()).append("\n");
                if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                    correctSb.append(opt.getKey()).append(" ");
                }
            }
        }
        return new FormattedOptions(optsSb.toString().trim(), correctSb.toString().trim());
    }

    private byte[] generateExamExcel(Exam exam, List<ExamQuestion> questions, boolean includeAnswerKey) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("De Thi");
            String[] headers = includeAnswerKey ? new String[] {"Câu số", "Câu hỏi", "Phương án", "Đáp án đúng"}
                    : new String[] {"Câu số", "Câu hỏi", "Phương án"};
            int[] widths = includeAnswerKey ? new int[] {11, 64, 57, 19} : new int[] {11, 64, 57};
            createWorksheetHeader(workbook, sheet, exam.getTitle() != null ? exam.getTitle() : "Đề thi",
                    subjectAndQuestionCount(exam.getSubject() != null ? exam.getSubject().getName() : "", questions.size()),
                    "1B344F", "EAF1F8", headers, widths);
            var white = exportCellStyle(workbook, "FFFFFF", "1B2938", 11, false);
            var tinted = exportCellStyle(workbook, "F1F5FA", "1B2938", 11, false);

            int rowIndex = EXCEL_HEADER_ROW + 1;
            for (ExamQuestion question : questions) {
                FormattedOptions formatted = formatExamQuestionOptions(question);
                String content = question.getQuestion().getContent();
                String[] values = includeAnswerKey
                        ? new String[] {content, formatted.optionsText(), formatted.correctAnswers()}
                        : new String[] {content, formatted.optionsText()};
                writeQuestionRow(sheet, rowIndex, (rowIndex - EXCEL_HEADER_ROW) % 2 == 1 ? white : tinted,
                        widths, question.getDisplayOrder(), values);
                rowIndex++;
            }
            sheet.setAutoFilter(new CellRangeAddress(EXCEL_HEADER_ROW, sheet.getLastRowNum(), 0, headers.length - 1));
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR, "Tạo Excel đề thi thất bại: " + e.getMessage());
        }
    }

    private byte[] generateQuestionsPdf(List<Question> questions, boolean includeAnswer) {
        try (InputStream templateStream = getClass().getResourceAsStream("/templates/reports/questions_template.jrxml")) {
            if (templateStream == null) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                        "Không tìm thấy file mẫu questions_template.jrxml trong resources");
            }
            JasperReport jasperReport = JasperCompileManager
                    .compileReport(templateStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_META", subjectAndQuestionCount(studySubjectName(questions), questions.size()));
            parameters.put("INCLUDE_ANSWER", includeAnswer);

            List<QuestionReportDto> reportDtos = new ArrayList<>();
            if (questions != null) {
                int count = 1;
                for (Question q : questions) {
                    List<String> questionImageUrls = q.getOwnedMedias() != null
                            ? q.getOwnedMedias().stream()
                                    .filter(m -> m
                                            .getMediaTarget() == MediaTarget.CONTENT)
                                    .map(QuestionMedia::getUrl)
                                    .collect(Collectors.toList())
                            : Collections.emptyList();

                    List<QuestionReportDto.OptionReportDto> optionDtos = new ArrayList<>();
                    List<QuestionOption> options = q.getOptions();
                    if (options != null) {
                        for (QuestionOption opt : options) {
                            optionDtos.add(
                                     QuestionReportDto.OptionReportDto.builder()
                                             .key(opt.getKey())
                                             .text(opt.getText())
                                             .mediaUrl(opt.getMediaUrl())
                                             .isCorrect(opt.getIsCorrect())
                                             .build());
                        }
                    }

                    String content = q.getContent() != null ? q.getContent() : "";
                    String explanation = q.getExplanation() != null ? q.getExplanation() : "";

                    reportDtos.add(QuestionReportDto.builder()
                            .order(count++)
                            .questionContent(content)
                            .questionImageUrls(questionImageUrls)
                            .options(optionDtos)
                            .explanation(explanation)
                            .build());
                }
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                    reportDtos);

            JasperPrint jasperPrint = JasperFillManager
                    .fillReport(jasperReport, parameters, dataSource);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("[DocumentExportService] Lỗi khi tạo PDF danh sách câu hỏi ôn tập bằng JasperReports: ", e);
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR,
                    "Tạo PDF câu hỏi ôn tập thất bại: " + e.getMessage());
        }
    }

    private byte[] generateExamPdf(Exam exam, List<ExamQuestion> questions, boolean includeAnswerKey) {
        try (InputStream templateStream = getClass().getResourceAsStream("/templates/reports/exam_template.jrxml")) {
            if (templateStream == null) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                        "Không tìm thấy file mẫu exam_template.jrxml trong resources");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("EXAM_TITLE", exam.getTitle() != null ? exam.getTitle() : "Đề thi");
            parameters.put("REPORT_META", subjectAndQuestionCount(
                    exam.getSubject() != null ? exam.getSubject().getName() : "", questions.size()));
            parameters.put("INCLUDE_ANSWER", includeAnswerKey);

            List<ExamQuestionReportDto> reportDtos = new ArrayList<>();
            for (ExamQuestion eq : questions) {
                FormattedOptions formatted = formatExamQuestionOptions(eq);

                List<String> questionImageUrls = eq.getQuestion().getOwnedMedias() != null
                        ? eq.getQuestion().getOwnedMedias().stream()
                                .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                                .map(QuestionMedia::getUrl)
                                .collect(Collectors.toList())
                        : Collections.emptyList();

                List<ExamQuestionReportDto.OptionReportDto> optionDtos = new ArrayList<>();
                List<QuestionOption> options = eq.getOptionsSnapshot() != null ? eq.getOptionsSnapshot()
                        : eq.getQuestion().getOptions();
                if (options != null) {
                    for (QuestionOption opt : options) {
                        optionDtos.add(ExamQuestionReportDto.OptionReportDto.builder()
                                .key(opt.getKey())
                                .text(opt.getText())
                                .mediaUrl(opt.getMediaUrl())
                                .isCorrect(opt.getIsCorrect())
                                .build());
                    }
                }

                reportDtos.add(ExamQuestionReportDto.builder()
                        .order(eq.getDisplayOrder())
                        .questionContent(eq.getQuestion().getContent())
                        .questionImageUrls(questionImageUrls)
                        .optionsText(formatted.optionsText())
                        .options(optionDtos)
                        .correctAnswer(formatted.correctAnswers())
                        .build());
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportDtos);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("[DocumentExportService] Lỗi khi tạo PDF bằng JasperReports: ", e);
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR, "Tạo PDF đề thi thất bại: " + e.getMessage());
        }
    }

}
