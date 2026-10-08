package com.frozenheart.backend.modules.media.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.media.MediaPurpose;
import com.frozenheart.backend.modules.media.dto.MediaUploadResponse;
import com.frozenheart.backend.modules.media.dto.MediaVerifyRequest;
import com.frozenheart.backend.modules.media.dto.MediaVerifyResponse;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest;
import com.frozenheart.backend.modules.media.dto.PresignMediaResponse;
import com.frozenheart.backend.modules.media.service.MediaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medias")
@RequiredArgsConstructor
@Tag(name = "27. Tải lên Media (Cloudflare R2)", description = "Các API tải lên tệp tin và hình ảnh: Upload trực tiếp, Cấp presigned URL, Xác minh tệp tồn tại")
public class MediaController {

    private final MediaService mediaService;

    @Operation(summary = "Tải lên media trực tiếp qua Backend", description = "Tải file trực tiếp qua multipart/form-data. File sẽ được lưu trữ lên Cloudflare R2 bucket và trả về URL công khai.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tải lên tệp thành công"),
            @ApiResponse(responseCode = "400", description = "File không được để trống hoặc thiếu tham số (INVALID_PARAMETER_VALUE)"),
            @ApiResponse(responseCode = "413", description = "Dung lượng tệp vượt quá giới hạn tối đa cho phép (FILE_SIZE_EXCEEDED)"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống khi tải tệp lên Cloudflare R2 (FILE_UPLOAD_FAILED)")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<MediaUploadResponse>> uploadMedia(
        @Parameter(description = "Tệp tin nhị phân cần tải lên", required = true)
        @RequestPart("file") MultipartFile file,

        @Parameter(description = "Mục đích sử dụng media (AVATAR, COVER, POST, QUESTION, CHAT...)", required = true)
        @RequestParam("purpose") MediaPurpose purpose
    ) {

        MediaUploadResponse response = mediaService.uploadMedia(file, purpose);

        return ResponseEntity.ok(GlobalResponse.success(response));

    }

    @Operation(summary = "Lấy URL ký trước (Presigned URLs)", description = "Client gửi danh sách tệp cần upload để nhận presigned PUT URL. Sau đó Client có thể upload trực tiếp lên Cloudflare R2 nhằm tối ưu băng thông cho Backend.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cấp presigned URL thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ (MISSING_REQUIRED_PARAMETER)"),
            @ApiResponse(responseCode = "413", description = "Có tệp tin vượt quá dung lượng cho phép (FILE_SIZE_EXCEEDED)"),
            @ApiResponse(responseCode = "500", description = "Lỗi sinh presigned URL từ MinIO/R2 (FILE_UPLOAD_FAILED)")
    })
    @PostMapping("/presign")
    public ResponseEntity<GlobalResponse<PresignMediaResponse>> getPresignedUrls(
            @Valid @RequestBody PresignMediaRequest request
    ) {

        PresignMediaResponse response = mediaService.generatePresignedUrls(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
        
    }

    @Operation(summary = "Xác minh các tệp tin đã upload lên R2", description = "Kiểm tra xem danh sách các object key hoặc URL đã tồn tại trên Cloudflare R2 hay chưa, trả về danh sách các tệp bị thiếu nếu có.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác minh hoàn tất"),
            @ApiResponse(responseCode = "400", description = "Danh sách URL không hợp lệ (MISSING_REQUIRED_PARAMETER)")
    })
    @PostMapping("/verify")
    public ResponseEntity<GlobalResponse<MediaVerifyResponse>> verifyMedias(
            @Valid @RequestBody MediaVerifyRequest request
    ) {
        List<String> missingUrls = mediaService.findMissingObjects(request.urls());
        return ResponseEntity.ok(GlobalResponse.success(new MediaVerifyResponse(missingUrls)));
    }

}
