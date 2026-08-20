package com.frozenheart.backend.core.dto;

import com.frozenheart.backend.core.constant.ResponseCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalResponse<T> {
    private String code;
    private String message;
    private T data;

    // Phản hồi thành công có data
    public static <T> GlobalResponse<T> success(T data) {
        return GlobalResponse.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    // Phản hồi thành công có data + message tùy chỉnh
    public static <T> GlobalResponse<T> success(String customMessage, T data) {
        return GlobalResponse.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(customMessage)
                .data(data)
                .build();
    }

    // Phản hồi thành công không có data (data = null)
    public static <T> GlobalResponse<T> success() {
        return GlobalResponse.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data(null)
                .build();
    }

    // Phản hồi lỗi dựa theo ResponseCode enum
    public static <T> GlobalResponse<T> error(ResponseCode responseCode) {
        return GlobalResponse.<T>builder()
                .code(responseCode.getCode())
                .message(responseCode.getMessage())
                .data(null)
                .build();
    }

    // Phản hồi lỗi tùy chỉnh message
    public static <T> GlobalResponse<T> error(ResponseCode responseCode, String customMessage) {
        return GlobalResponse.<T>builder()
                .code(responseCode.getCode())
                .message(customMessage)
                .data(null)
                .build();
    }

    // Phản hồi lỗi tùy chỉnh message có data
    public static <T> GlobalResponse<T> error(ResponseCode responseCode, String customMessage, T data) {
        return GlobalResponse.<T>builder()
                .code(responseCode.getCode())
                .message(customMessage)
                .data(data)
                .build();
    }
}
