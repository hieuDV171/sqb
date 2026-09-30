package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.modules.session.dto.ExcelParsedClassData;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelParserService {

    ExcelParsedClassData parseCourseClassExcel(MultipartFile file);

}
