package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.ExamQuestion;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.ExamQuestionReportDto;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.QuestionReportDto;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    private final MinioClient minioClient;
    private final R2Properties r2Properties;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportQuestions(Long subjectId, String format, Boolean includeAnswer) {
        boolean withAnswer = includeAnswer == null || includeAnswer;
        String ext = "excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format) ? "xlsx" : "pdf";

        // Đặt tên chung như thế này để nếu có người cùng tải về 1 loại thì dùng cái này cache luôn
        String objectKey = String.format("exports/questions/subject_%d_ans_%b.%s", subjectId, withAnswer, ext);

        // Fetch Questions (JOIN FETCH session, subject, ownedMedias)
        List<Question> questions = questionRepository.findAllBySubjectIdForExport(subjectId);
        if (questions.isEmpty()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Không tìm thấy câu hỏi nào thuộc môn học này");
        }

        // Check MinIO Cache
        ExportResponse cachedResponse = checkMinioCache(objectKey, questions.size());
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
        return uploadAndPresign(objectKey, contentBytes, contentType, questions.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResponse exportExam(Exam exam, String format, Boolean includeAnswerKey, String paperSize) {
        boolean withAnswer = includeAnswerKey != null && includeAnswerKey;
        String ext = "excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format) ? "xlsx" : "pdf";
        String objectKey = String.format("exports/exams/exam_%d_ans_%b.%s", exam.getId(), withAnswer, ext);

        int questionCount = exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0;

        // Check MinIO Cache
        ExportResponse cachedResponse = checkMinioCache(objectKey, questionCount);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // Render Document
        byte[] contentBytes;
        String contentType;
        if ("xlsx".equalsIgnoreCase(ext)) {
            contentBytes = generateExamExcel(exam, withAnswer);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            contentBytes = generateExamPdf(exam, withAnswer);
            contentType = "application/pdf";
        }

        // Upload to MinIO & Presign
        return uploadAndPresign(objectKey, contentBytes, contentType, exam.getExamQuestions().size());
    }

    private ExportResponse checkMinioCache(String objectKey, int questionsCount) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(r2Properties.getBucketName())
                            .object(objectKey)
                            .build());
            if (stat != null) {
                String downloadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(r2Properties.getBucketName())
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

    private ExportResponse uploadAndPresign(String objectKey, byte[] contentBytes, String contentType,
            int questionsCount) {
        try {
            try (InputStream is = new ByteArrayInputStream(contentBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(r2Properties.getBucketName())
                                .object(objectKey)
                                .stream(is, (long) contentBytes.length, -1L)
                                .contentType(contentType)
                                .build());
            }

            String downloadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(r2Properties.getBucketName())
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

        } catch (Exception e) {
            log.error("[DocumentExportService] Lỗi khi upload/presign file export: ", e);
            throw new AppException(ResponseCode.FILE_UPLOAD_FAILED, "Xuất file thất bại: " + e.getMessage());
        }
    }

    // --- Render Methods ---

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return headerStyle;
    }

    private CellStyle createContentStyle(Workbook workbook) {
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
        return contentStyle;
    }

    private byte[] generateQuestionsExcel(List<Question> questions, boolean includeAnswer) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh Sach Cau Hoi");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle contentStyle = createContentStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = includeAnswer ? new String[] { "STT", "Nội Dung", "Các Lựa Chọn", "Giải Thích" }
                    : new String[] { "STT", "Nội Dung", "Các Lựa Chọn" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Question q : questions) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0);
                c0.setCellValue(rowIdx - 1);
                c0.setCellStyle(contentStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(q.getContent() != null ? q.getContent() : "");
                c1.setCellStyle(contentStyle);

                StringBuilder optionsSb = new StringBuilder();
                List<QuestionOption> options = q.getOptions();
                if (options != null) {
                    for (QuestionOption opt : options) {
                        optionsSb.append(opt.getKey()).append(". ").append(opt.getText());
                        if (includeAnswer && Boolean.TRUE.equals(opt.getIsCorrect())) {
                            optionsSb.append(" (Đúng)");
                        }
                        optionsSb.append("\n");
                    }
                }
                Cell c2 = row.createCell(2);
                c2.setCellValue(optionsSb.toString().trim());
                c2.setCellStyle(contentStyle);

                if (includeAnswer) {
                    Cell c3 = row.createCell(3);
                    c3.setCellValue(q.getExplanation() != null ? q.getExplanation() : "");
                    c3.setCellStyle(contentStyle);
                }
            }

            sheet.setColumnWidth(0, 10 * 256);
            sheet.setColumnWidth(1, 55 * 256);
            sheet.setColumnWidth(2, 45 * 256);
            if (includeAnswer) {
                sheet.setColumnWidth(3, 30 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR, "Tạo Excel thất bại: " + e.getMessage());
        }
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

    private byte[] generateExamExcel(Exam exam, boolean includeAnswerKey) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("De Thi");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle contentStyle = createContentStyle(workbook);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ĐỀ THI: " + (exam.getTitle() != null ? exam.getTitle() : ""));
            titleCell.setCellStyle(headerStyle);

            Row subRow = sheet.createRow(1);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Môn học: " + (exam.getSubject() != null ? exam.getSubject().getName() : ""));

            Row headerRow = sheet.createRow(3);
            String[] headers = includeAnswerKey
                    ? new String[] { "Câu Số", "Nội Dung Câu Hỏi", "Các Phương Án Lựa Chọn", "Đáp Án Đúng" }
                    : new String[] { "Câu Số", "Nội Dung Câu Hỏi", "Các Phương Án Lựa Chọn" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            if (exam.getExamQuestions() != null) {
                for (ExamQuestion eq : exam.getExamQuestions()) {
                    Row row = sheet.createRow(rowIdx++);

                    Cell c0 = row.createCell(0);
                    c0.setCellValue("Câu " + eq.getDisplayOrder());
                    c0.setCellStyle(contentStyle);

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(eq.getQuestion().getContent());
                    c1.setCellStyle(contentStyle);

                    FormattedOptions formatted = formatExamQuestionOptions(eq);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(formatted.optionsText());
                    c2.setCellStyle(contentStyle);

                    if (includeAnswerKey) {
                        Cell c3 = row.createCell(3);
                        c3.setCellValue(formatted.correctAnswers());
                        c3.setCellStyle(contentStyle);
                    }
                }
            }

            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 55 * 256);
            sheet.setColumnWidth(2, 45 * 256);
            if (includeAnswerKey) {
                sheet.setColumnWidth(3, 20 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(ResponseCode.UNKNOWN_SYSTEM_ERROR, "Tạo Excel Đề thi thất bại: " + e.getMessage());
        }
    }

    private byte[] generateQuestionsPdf(List<Question> questions, boolean includeAnswer) {
        try {
            InputStream templateStream = getClass().getResourceAsStream("/templates/reports/questions_template.jrxml");
            if (templateStream == null) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                        "Không tìm thấy file mẫu questions_template.jrxml trong resources");
            }
            JasperReport jasperReport = JasperCompileManager
                    .compileReport(templateStream);

            String subjectName = (questions != null && !questions.isEmpty() && questions.getFirst().getSession() != null
                    && questions.getFirst().getSession().getSubject() != null)
                            ? questions.getFirst().getSession().getSubject().getName()
                            : "";

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("SUBJECT_NAME", subjectName);
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

    private byte[] generateExamPdf(Exam exam, boolean includeAnswerKey) {
        try {
            InputStream templateStream = getClass().getResourceAsStream("/templates/reports/exam_template.jrxml");
            if (templateStream == null) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                        "Không tìm thấy file mẫu exam_template.jrxml trong resources");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("EXAM_TITLE", exam.getTitle() != null ? exam.getTitle() : "ĐỀ THI");
            parameters.put("SUBJECT_NAME", exam.getSubject() != null ? exam.getSubject().getName() : "");
            parameters.put("INCLUDE_ANSWER", includeAnswerKey);

            List<ExamQuestionReportDto> reportDtos = new ArrayList<>();
            if (exam.getExamQuestions() != null) {
                for (ExamQuestion eq : exam.getExamQuestions()) {
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
