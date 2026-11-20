package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.exception.AppException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class ExcelParserPasswordRetirementTest {
    private final ExcelParserServiceImpl parser = new ExcelParserServiceImpl();

    @Test void parsesDemographicsAndMissingDobWithoutDefaultPasswordConfiguration() throws Exception {
        var dated = parser.parseCourseClassExcel(file("15/05/2003")).students().getFirst();
        assertThat(dated.dateOfBirth()).isEqualTo(LocalDate.of(2003, 5, 15));
        assertThat(dated.studentCode()).isEqualTo("00123");
        assertThat(parser.parseCourseClassExcel(file("")).students().getFirst().dateOfBirth()).isNull();
    }

    @Test void impossibleDatesStillRejectTheFileRatherThanSilentlyChangingDemographics() throws Exception {
        var input = file("31/02/2003");
        assertThatThrownBy(() -> parser.parseCourseClassExcel(input)).isInstanceOf(AppException.class);
    }

    private MockMultipartFile file(String date) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Danh sách");
            var header = sheet.createRow(0);
            String[] headings = {"MSSV", "Họ và tên", "Ngày sinh", "Email"};
            for (int i = 0; i < headings.length; i++) header.createCell(i).setCellValue(headings[i]);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("00123");
            row.createCell(1).setCellValue("Sinh viên kiểm thử");
            row.createCell(2).setCellValue(date);
            row.createCell(3).setCellValue("fixture@example.edu");
            workbook.write(output);
            return new MockMultipartFile("file", "171146-IT4409.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
