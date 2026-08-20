package com.frozenheart.backend.core.exception;

import com.frozenheart.backend.core.constant.ResponseCode;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ResponseCode responseCode;

    public AppException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public AppException(ResponseCode responseCode, String customMessage) {
        super(customMessage);
        this.responseCode = responseCode;
    }
}
