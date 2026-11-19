package com.frozenheart.backend.modules.media.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.modules.media.dto.LectureVideoUpload;
import com.frozenheart.backend.modules.media.service.LectureVideoUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/lecturer/medias/lecture-uploads")
@RequiredArgsConstructor
@Tag(name = "Tải video bài giảng", description = "Giảng viên/ADMIN tải MP4/WebM trực tiếp lên R2 theo multipart. Client tạo UUID phiên, khởi tạo, xin URL từng phần, PUT binary lên R2 rồi complete; dùng media trả về khi tạo/cập nhật bài giảng.")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn Bearer AT"),
        @ApiResponse(responseCode = "403", description = "Không có quyền giảng viên/ADMIN hoặc phiên thuộc người khác"),
        @ApiResponse(responseCode = "400", description = "UUID, metadata, số phần hoặc trạng thái tải không hợp lệ"),
        @ApiResponse(responseCode = "507", description = "Vượt quota ghi R2 toàn tài khoản (1017)"),
        @ApiResponse(responseCode = "503", description = "Quota chưa sẵn sàng (1018) hoặc kho lưu trữ không khả dụng")
})
public class LectureVideoUploadController {
    private final LectureVideoUploadService service;

    @Operation(summary = "Khởi tạo hoặc khôi phục phiên tải video", description = "Client sinh UUID ổn định cho một tệp. Gọi lại cùng UUID/metadata trả trạng thái hiện có; metadata khác hoặc phiên đã hủy bị từ chối. Nhận MP4/WebM, dung lượng tối đa theo LECTURE_VIDEO_MAX_FILE_SIZE (mặc định 2 GiB); phiên sống 24 giờ, mỗi phần 16 MiB trừ phần cuối. Giữ chỗ quota trước khi cấp quyền ghi; chưa upload binary ở request này.")
    @ApiResponse(responseCode = "200", description = "Trạng thái phiên gồm part_size theo byte, uploaded_parts đã có và media nếu hoàn tất")
    @PutMapping("/{id}")
    public GlobalResponse<LectureVideoUpload.Status> start(
            @Parameter(description = "UUID do client sinh cho phiên; giữ lại để resume", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID id,
            @Valid @RequestBody LectureVideoUpload.StartRequest request) {
        return GlobalResponse.success(service.start(id, request));
    }

    @Operation(summary = "Xem tiến độ tải video để tiếp tục", description = "Đọc các phần đã tồn tại trên R2 và media đã xác minh nếu hoàn tất. Chỉ chủ phiên giảng viên/ADMIN được truy cập; không dùng kết quả local của client làm bằng chứng tải thành công.")
    @ApiResponse(responseCode = "200", description = "Trạng thái upload; uploaded_parts dùng số thứ tự từ 1")
    @GetMapping("/{id}")
    public GlobalResponse<LectureVideoUpload.Status> status(
            @Parameter(description = "UUID phiên tải cần kiểm tra", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID id) {
        return GlobalResponse.success(service.status(id));
    }

    @Operation(summary = "Xin URL PUT cho một phần video", description = "Chủ phiên nhận URL có chữ ký, tối đa 15 phút và không vượt hạn phiên. Client PUT đúng byte của phần trực tiếp lên URL này, không gửi Bearer SQB sang R2; số phần bắt đầu từ 1, phần cuối có thể nhỏ hơn part_size. URL ràng buộc độ dài phần. Có thể xin lại URL để retry phần chưa xong.")
    @ApiResponse(responseCode = "200", description = "data.upload_url dùng PUT binary trực tiếp R2")
    @PostMapping("/{id}/parts/{number}")
    public GlobalResponse<LectureVideoUpload.PartUrl> partUrl(
            @Parameter(description = "UUID phiên tải", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID id,
            @Parameter(description = "Số thứ tự phần từ 1 đến ceil(file_size/part_size)", example = "1") @PathVariable int number) {
        return GlobalResponse.success(service.partUrl(id, number));
    }

    @Operation(summary = "Ghép và xác minh video đã tải đủ", description = "Chỉ gọi sau khi tất cả PUT phần thành công. Backend tự đọc manifest/ETag từ R2, kiểm số phần/kích thước rồi hoàn tất và xác minh object. Có thể gọi lại khi đã READY để nhận cùng media. Gắn MediaItem trả về vào bài giảng qua API bài viết; complete chưa đồng nghĩa video đã được gắn vĩnh viễn vào một bài.")
    @ApiResponse(responseCode = "200", description = "MediaItem video đã xác minh để gắn vào bài giảng")
    @PostMapping("/{id}/complete")
    public GlobalResponse<MediaItem> complete(
            @Parameter(description = "UUID phiên cần hoàn tất", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID id) {
        return GlobalResponse.success(service.complete(id));
    }

    @Operation(summary = "Hủy phiên tải video", description = "Chủ phiên hủy multipart đang tải; gọi lại an toàn. UUID chưa khởi tạo cũng được đánh dấu hủy để tránh race khi start đến muộn. Không xóa video đã gắn vào bài. Phản hồi không có GlobalResponse/body.")
    @ApiResponse(responseCode = "204", description = "Đã hủy hoặc không còn việc cần hủy; body rỗng")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "UUID phiên cần hủy", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
