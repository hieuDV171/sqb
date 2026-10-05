package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.user.Gender;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.dto.ExcelParsedClassData;
import com.frozenheart.backend.modules.session.dto.ExcelStudentRow;
import com.frozenheart.backend.modules.session.service.ExcelParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExcelParserServiceImpl implements ExcelParserService {

    private static final Pattern DOB_PATTERN = Pattern.compile("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})");
    private static final Pattern MAJOR_PATTERN = Pattern.compile("^(.*?)(?:\\s+\\d+)?-K\\d+$", Pattern.CASE_INSENSITIVE);

    @Value("${app.security.default-user-password}")
    private String defaultUserPassword;

    @Override
    public ExcelParsedClassData parseCourseClassExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "File Excel không được để trống");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 0) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Sheet đầu tiên của file Excel không có dữ liệu");
            }

            // 1. Tìm dòng tiêu đề (Header Row)
            int headerRowNum = findHeaderRow(sheet);
            if (headerRowNum == -1) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không tìm thấy dòng tiêu đề hợp lệ trong file Excel");
            }

            Row headerRow = sheet.getRow(headerRowNum);
            Map<String, Integer> colMap = resolveColumns(headerRow);

            // Kiểm tra các cột cốt lõi bắt buộc
            if (!colMap.containsKey("mssv") && !colMap.containsKey("email")) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "File Excel thiếu cột MSSV hoặc Email");
            }

            String semesterName = null;
            String department = null;
            String classCode = null;
            String classType = null;
            String subjectCode = null;
            String subjectName = null;
            String lecturerName = null;

            List<ExcelStudentRow> students = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            // 2. Đọc từng dòng dữ liệu sinh viên
            for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                String mssv = getCellValue(row, colMap.get("mssv"), formatter);
                String email = getCellValue(row, colMap.get("email"), formatter);
                String fullName = getCellValue(row, colMap.get("fullname"), formatter);

                // Dòng không có cả MSSV lẫn email thì coi như dòng phụ/trống
                if ((mssv == null || mssv.isBlank()) && (email == null || email.isBlank())) {
                    continue;
                }

                // Trích xuất metadata lớp học phần từ dòng dữ liệu đầu tiên nếu chưa có
                if (semesterName == null) {
                    semesterName = getCellValue(row, colMap.get("semester"), formatter);
                }
                if (department == null) {
                    department = getCellValue(row, colMap.get("department"), formatter);
                }
                if (classCode == null) {
                    classCode = getCellValue(row, colMap.get("classcode"), formatter);
                }
                if (classType == null) {
                    classType = getCellValue(row, colMap.get("classtype"), formatter);
                }
                if (subjectCode == null) {
                    subjectCode = getCellValue(row, colMap.get("subjectcode"), formatter);
                }
                if (subjectName == null) {
                    subjectName = getCellValue(row, colMap.get("subjectname"), formatter);
                }
                if (lecturerName == null) {
                    lecturerName = getCellValue(row, colMap.get("lecturer"), formatter);
                }

                // Bóc tách ngày sinh và tạo mật khẩu mặc định ddMMyyyy
                ParsedDob parsedDob = parseDateOfBirth(row.getCell(colMap.getOrDefault("dob", -1)), formatter, mssv);

                // Giới tính
                String rawGender = getCellValue(row, colMap.get("gender"), formatter);
                Gender gender = parseGender(rawGender);

                // Tên lớp sinh hoạt và bóc tách ngành (major)
                String rawClassName = getCellValue(row, colMap.get("classname"), formatter);
                String major = extractMajorFromClassName(rawClassName);

                // Chuẩn hóa email nếu trống mà có MSSV (theo format HUST)
                if ((email == null || email.isBlank()) && mssv != null && !mssv.isBlank()) {
                    email = generateHustEmail(fullName, mssv);
                }

                students.add(ExcelStudentRow.builder()
                        .rowNumber(r + 1)
                        .studentCode(cleanString(mssv))
                        .fullName(cleanString(fullName))
                        .gender(gender)
                        .dateOfBirth(parsedDob.dateOfBirth())
                        .defaultPassword(parsedDob.defaultPassword())
                        .email(email != null ? email.trim() : "")
                        .className(cleanString(rawClassName))
                        .major(cleanString(major))
                        .build());
            }

            // Nếu chưa tìm thấy classCode từ các ô trong sheet, trích xuất từ tên file (ví dụ: 171146-IT4409.xlsx -> 171146)
            if (classCode == null || classCode.isBlank()) {
                classCode = extractClassCodeFromFileName(file.getOriginalFilename());
            }

            // Nếu chưa tìm thấy subjectCode từ các ô trong sheet, trích xuất từ tên file
            if (subjectCode == null || subjectCode.isBlank()) {
                subjectCode = extractSubjectCodeFromFileName(file.getOriginalFilename());
            }

            log.info("[ExcelParser] Đã đọc thành công file Excel: classCode={}, subjectCode={}, semester={}, totalStudents={}",
                    classCode, subjectCode, semesterName, students.size());

            return ExcelParsedClassData.builder()
                    .semesterName(cleanString(semesterName))
                    .department(cleanString(department))
                    .classCode(cleanString(classCode))
                    .classType(cleanString(classType))
                    .subjectCode(cleanString(subjectCode))
                    .subjectName(cleanString(subjectName))
                    .lecturerName(cleanString(lecturerName))
                    .students(students)
                    .build();

        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("[ExcelParser] Lỗi khi đọc file Excel: {}", e.getMessage(), e);
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Không thể đọc cấu trúc file Excel: " + e.getMessage());
        }
    }

    private int findHeaderRow(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 15); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String val = formatter.formatCellValue(row.getCell(c)).trim().toLowerCase();
                if (val.contains("mssv") || val.contains("mã lớp") || val.contains("mã học") || val.contains("mã môn")) {
                    return r;
                }
            }
        }
        return -1;
    }

    private Map<String, Integer> resolveColumns(Row headerRow) {
        DataFormatter formatter = new DataFormatter();
        Map<String, Integer> colMap = new HashMap<>();

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell).trim().toLowerCase();

            if (text.contains("học kỳ") || text.contains("hoc ky") || text.contains("semester")) {
                colMap.putIfAbsent("semester", c);
            } else if (text.contains("đv giảng dạy") || text.contains("đơn vị giảng dạy") || text.contains("khoa/viện")) {
                colMap.putIfAbsent("department", c);
            } else if (text.contains("loại lớp")) {
                colMap.putIfAbsent("classtype", c);
            } else if (text.contains("mã học phần") || text.contains("ma hoc phan") || text.contains("mã học") || text.contains("ma hoc") || text.contains("mã hp") || text.contains("ma hp") || text.contains("mã môn") || text.contains("ma mon")) {
                colMap.putIfAbsent("subjectcode", c);
            } else if (text.contains("tên học phần") || text.contains("ten hoc phan") || text.contains("tên hp") || text.contains("ten hp") || text.contains("tên môn") || text.contains("ten mon")) {
                colMap.putIfAbsent("subjectname", c);
            } else if (text.contains("mã lớp") && !text.contains("mã lớp thi")) {
                colMap.putIfAbsent("classcode", c);
            } else if (text.contains("mssv") || text.contains("mã số sinh viên") || text.contains("mã sv") || text.contains("mã sinh viên")) {
                colMap.putIfAbsent("mssv", c);
            } else if (text.contains("họ và tên sv") || text.contains("họ và tên") || text.contains("họ tên") || text.contains("tên sv")) {
                colMap.putIfAbsent("fullname", c);
            } else if (text.contains("giới tính") || text.contains("gioi tinh")) {
                colMap.putIfAbsent("gender", c);
            } else if (text.contains("ngày sinh") || text.contains("ngay sinh") || text.contains("dob")) {
                colMap.putIfAbsent("dob", c);
            } else if (text.contains("email")) {
                colMap.putIfAbsent("email", c);
            } else if (text.contains("tên lớp") || text.contains("lớp sh") || text.contains("lớp quản lý")) {
                colMap.putIfAbsent("classname", c);
            } else if (text.contains("gv giảng dạy") || text.contains("giảng viên")) {
                colMap.putIfAbsent("lecturer", c);
            }
        }

        // Chỉ fallback sang vị trí cột cố định nếu hoàn toàn không nhận diện được cột nào qua header
        if (colMap.isEmpty()) {
            colMap.put("semester", 0);
            colMap.put("department", 1);
            colMap.put("classcode", 2);
            colMap.put("classtype", 3);
            colMap.put("subjectcode", 4);
            colMap.put("subjectname", 5);
            colMap.put("mssv", 6);
            colMap.put("fullname", 7);
            colMap.put("gender", 8);
            colMap.put("dob", 9);
            colMap.put("email", 10);
            colMap.put("classname", 11);
            colMap.put("lecturer", 19);
        }

        return colMap;
    }

    public static String extractClassCodeFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        // Tìm dãy 5-7 chữ số (chuẩn mã lớp học phần HUST, ví dụ 171146-IT4409.xlsx -> 171146)
        Matcher matcher = Pattern.compile("(?<!\\d)(\\d{5,7})(?!\\d)").matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String extractSubjectCodeFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        // Tìm mã môn học (ví dụ: IT4409, MI1111, ED3220, ...)
        Matcher matcher = Pattern.compile("(?i)(?<![A-Z0-9])([A-Z]{2,4}\\d{4})(?![A-Z0-9])").matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return "";
    }

    private ParsedDob parseDateOfBirth(Cell cell, DataFormatter formatter, String fallbackMssv) {
        if (cell != null) {
            try {
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    LocalDate ld = cell.getLocalDateTimeCellValue().toLocalDate();
                    String pwd = ld.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
                    return new ParsedDob(ld, pwd);
                }
            } catch (Exception ignored) {}

            String text = formatter.formatCellValue(cell).trim();
            if (!text.isBlank()) {
                Matcher m = DOB_PATTERN.matcher(text);
                if (m.find()) {
                    int day = Integer.parseInt(m.group(1));
                    int month = Integer.parseInt(m.group(2));
                    int year = Integer.parseInt(m.group(3));
                    LocalDate ld = LocalDate.of(year, month, day);
                    String pwd = String.format("%02d%02d%04d", day, month, year);
                    return new ParsedDob(ld, pwd);
                }
            }
        }

        // Fallback nếu không có ngày sinh hợp lệ: dùng MSSV hoặc mật khẩu mặc định an toàn
        String fallbackPwd = (fallbackMssv != null && !fallbackMssv.isBlank()) ? fallbackMssv.trim() : defaultUserPassword;
        return new ParsedDob(null, fallbackPwd);
    }

    private Gender parseGender(String rawGender) {
        if (rawGender == null || rawGender.isBlank()) {
            return null;
        }
        String lower = rawGender.trim().toLowerCase();
        if (lower.contains("nam") || lower.contains("male") || lower.equals("m")) {
            return Gender.MALE;
        }
        if (lower.contains("nữ") || lower.contains("nu") || lower.contains("female") || lower.equals("f")) {
            return Gender.FEMALE;
        }
        return null;
    }

    private String extractMajorFromClassName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        Matcher matcher = MAJOR_PATTERN.matcher(className.trim());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return className.trim();
    }

    private String getCellValue(Row row, Integer colIndex, DataFormatter formatter) {
        if (colIndex == null || colIndex < 0 || row.getCell(colIndex) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(colIndex)).trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String cleanString(String str) {
        return str != null ? str.trim() : "";
    }

    public static String generateHustEmail(String fullName, String mssv) {
        if (mssv == null || mssv.isBlank()) {
            return "";
        }
        String cleanMssv = mssv.trim();
        // Cắt 2 ký tự đầu của MSSV (ví dụ: 202512345 -> 2512345, 20235001 -> 235001)
        String shortMssv = cleanMssv.length() > 2 ? cleanMssv.substring(2) : cleanMssv;

        if (fullName == null || fullName.isBlank()) {
            return shortMssv + "@sis.hust.edu.vn";
        }

        String normalized = removeDiacritics(fullName.trim());
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0) {
            return shortMssv + "@sis.hust.edu.vn";
        }

        // Tên sinh viên (từ cuối cùng)
        String lastName = parts[parts.length - 1];
        String formattedLastName = lastName.isEmpty() ? ""
                : Character.toUpperCase(lastName.charAt(0)) + lastName.substring(1).toLowerCase();

        // Viết tắt của các từ còn lại (họ và tên đệm)
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }

        return formattedLastName + "." + initials + shortMssv + "@sis.hust.edu.vn";
    }

    public static String removeDiacritics(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replace("Đ", "D")
                .replace("đ", "d");
    }

    private record ParsedDob(LocalDate dateOfBirth, String defaultPassword) {}
}
