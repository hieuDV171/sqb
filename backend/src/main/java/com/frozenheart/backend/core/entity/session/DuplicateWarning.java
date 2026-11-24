package com.frozenheart.backend.core.entity.session;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Kết quả đối sánh giữa câu hỏi đang được dò trùng và một câu hỏi khác")
public record DuplicateWarning(
        @Schema(description = "ID câu hỏi đang được dò trùng", example = "105")
        Long questionId,
        @Schema(description = "Vị trí câu hỏi trong phiên nếu có dữ liệu vị trí. Dùng question_id để xác định câu hỏi.", example = "3")
        Integer questionIndex,
        @Schema(description = "ID câu hỏi được tìm thấy khi đối sánh", example = "208")
        Long similarQuestionId,
        @Schema(description = "Phạm vi của câu đối sánh, trong cùng phiên hoặc trong ngân hàng câu hỏi", example = "QUESTION_BANK")
        Scope scope,
        @Schema(description = "Loại kết quả đối sánh, gồm trùng hoàn toàn, trùng nội dung nhưng khác đáp án đúng hoặc tương tự", example = "SIMILAR")
        Kind kind,
        @Schema(description = "Điểm tương đồng của phép so sánh trigram hoặc embedding văn bản, điểm cao hơn thể hiện mức tương đồng cao hơn", example = "0.93")
        Double similarityScore,
        @Schema(description = "Số bit khác nhau giữa hai mã băm cảm nhận của ảnh trong kết quả P_HASH", example = "3")
        Integer hammingDistance,
        @Schema(description = "Phương pháp phát hiện dùng để tạo kết quả đối sánh này", example = "TRIGRAM")
        DuplicateDetectionTier tier,
        @Schema(description = "Đoạn đầu nội dung đã chuẩn hóa của câu hỏi đối sánh, tối đa 240 ký tự", example = "Thuật toán định thời CPU nào có thể gây đói tiến trình?")
        @JsonAlias({"matchedSnipet", "matched_snipet"}) String matchedSnippet,
        @Schema(description = "URL ảnh của câu hỏi đối sánh trong kết quả P_HASH", example = "https://media.sqb.edu.vn/questions/scheduling-diagram.png")
        String mediaUrl) {

    public enum Scope { SAME_SESSION, QUESTION_BANK }
    public enum Kind { EXACT, ANSWER_CONFLICT, SIMILAR }
}
