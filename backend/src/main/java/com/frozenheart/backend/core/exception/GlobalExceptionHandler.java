package com.frozenheart.backend.core.exception;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.error.FieldErrorDetail;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getResponseCode().getHttpStatus())
                .body(GlobalResponse.error(ex.getResponseCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(GlobalResponse.error(ResponseCode.ACCESS_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(GlobalResponse.error(ResponseCode.ACCESS_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<List<FieldErrorDetail>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> errors = getErrors(ex.getBindingResult());
        String defaultMsg = "Dữ liệu yêu cầu không hợp lệ";
        if (!errors.isEmpty()) {
            FieldErrorDetail firstError = errors.get(0);
            if (firstError.message() != null && !firstError.message().isBlank()) {
                defaultMsg = "Trường '" + firstError.field() + "' không hợp lệ (" + firstError.message() + ")";
            }
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.MISSING_REQUIRED_PARAMETER, defaultMsg, errors));
    }

    private List<FieldErrorDetail> getErrors(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> {
                    String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
                    return new FieldErrorDetail(fieldName, error.getDefaultMessage());
                })
                .toList();
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
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_VALUE, "Dữ liệu tham số không hợp lệ", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<GlobalResponse<List<FieldErrorDetail>>> handleBindException(BindException ex) {
        List<FieldErrorDetail> errors = getErrors(ex.getBindingResult());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.MISSING_REQUIRED_PARAMETER, "Liên kết dữ liệu thất bại", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponse<Void>> handleHttpMessageNotReadableException() {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_TYPE, "Dữ liệu JSON gửi lên không đúng định dạng"));
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
    public ResponseEntity<GlobalResponse<Void>> handleDataIntegrityViolationException() {
        String message = "Dữ liệu bị vi phạm ràng buộc cơ sở dữ liệu";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(GlobalResponse.error(ResponseCode.INVALID_PARAMETER_VALUE, message));
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<GlobalResponse<Void>> handleOptimisticLockingFailureException(Exception ex) {
        String message = "Dữ liệu đã được cập nhật bởi một thao tác khác trong cùng thời điểm. Vui lòng tải lại trang và thử lại.";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(GlobalResponse.error(ResponseCode.STALE_DATA_DETECTED, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleGenericException(Exception ex) {
        log.error("[UnhandledException] TraceId={}: ", MDC.get("traceId"), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(ResponseCode.UNHANDLED_EXCEPTION, ex.getMessage() != null ? ex.getMessage() : ResponseCode.UNHANDLED_EXCEPTION.getMessage()));
    }
}

