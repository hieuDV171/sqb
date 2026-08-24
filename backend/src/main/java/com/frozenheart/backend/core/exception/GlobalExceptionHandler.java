package com.frozenheart.backend.core.exception;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getResponseCode().getHttpStatus())
                .body(GlobalResponse.error(ex.getResponseCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.MISSING_REQUIRED_PARAMETER, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_VALUE, "Validation failed", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.MISSING_REQUIRED_PARAMETER, "Binding failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_TYPE, "Malformed JSON request body"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GlobalResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(GlobalResponse.error(ResponseCode.METHOD_NOT_ALLOWED, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(GlobalResponse.error(ResponseCode.RESOURCE_NOT_FOUND, "Resource not found: " + ex.getResourcePath()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc cơ sở dữ liệu";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMsg = ex.getCause().getMessage();
            if (causeMsg.contains("idx_semesters_single_active")) {
                message = "Đã có 1 học kỳ khác đang trong trạng thái kích hoạt";
            }
        }
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_VALUE, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(ResponseCode.UNHANDLED_EXCEPTION, ex.getMessage() != null ? ex.getMessage() : ResponseCode.UNHANDLED_EXCEPTION.getMessage()));
    }
}

