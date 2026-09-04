package com.frozenheart.backend.modules.search.dto;

/**
 * Ngữ cảnh tìm kiếm câu hỏi:
 * - PUBLIC: Tìm kiếm trên thanh chung (Toàn trường xem bản gốc do sinh viên đề xuất original_)
 * - CORE: Tìm kiếm chuyên sâu trong Ngân hàng đề thi (Chỉ dành cho Giảng viên, trả bản core_ đã được duyệt)
 */
public enum SearchScope {
    PUBLIC,
    CORE
}
