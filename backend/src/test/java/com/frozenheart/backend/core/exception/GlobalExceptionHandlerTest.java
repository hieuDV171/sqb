package com.frozenheart.backend.core.exception;

import com.frozenheart.backend.core.dto.GlobalResponse;
import org.hibernate.PropertyValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import io.jsonwebtoken.MalformedJwtException;

import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest(name = "{0} phải dùng cùng fallback an toàn")
    @MethodSource("integrityViolations")
    void shouldUseSafeServerErrorFallback(
            String scenario,
            DataIntegrityViolationException exception,
            String sensitiveDetail) {

        ResponseEntity<GlobalResponse<Void>> response =
                handler.handleDataIntegrityViolationException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("9999", response.getBody().getCode());
        assertEquals("Không thể lưu dữ liệu do lỗi hệ thống. Vui lòng thử lại sau.", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains(sensitiveDetail));
    }

    @Test
    @DisplayName("Generic handler không được trả raw exception message cho client")
    void shouldNotExposeGenericExceptionMessage() {
        String sensitiveDetail = "classloader-and-internal-class-name";

        ResponseEntity<GlobalResponse<Void>> response =
                handler.handleGenericException(new ClassCastException(sensitiveDetail));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("9999", response.getBody().getCode());
        assertEquals(
                "Hệ thống gặp lỗi khi xử lý yêu cầu. Vui lòng thử lại sau.",
                response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains(sensitiveDetail));
    }

    @Test
    @DisplayName("Sai kiểu query parameter phải trả 400 thay vì generic 500")
    void shouldMapArgumentTypeMismatchToBadRequest() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("limit");

        ResponseEntity<GlobalResponse<Void>> response =
                handler.handleMethodArgumentTypeMismatchException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1003", response.getBody().getCode());
    }

    @Test
    @DisplayName("JWT sai định dạng phải trả 401 thay vì generic 500")
    void shouldMapMalformedJwtToUnauthorized() {
        ResponseEntity<GlobalResponse<Void>> response =
                handler.handleJwtException(new MalformedJwtException("sensitive-token-detail"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("9998", response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("sensitive-token-detail"));
    }

    private static Stream<Arguments> integrityViolations() {
        String constraintName = "constraint_sensitive_name";
        String propertyName = "displayOrder";

        return Stream.of(
                Arguments.of(
                        "Vi phạm unique constraint từ PostgreSQL",
                        new DataIntegrityViolationException(
                                "Không thể ghi dữ liệu",
                                new SQLException("Unique constraint: " + constraintName, "23505")),
                        constraintName),
                Arguments.of(
                        "Thuộc tính bắt buộc bị null trong Hibernate",
                        new DataIntegrityViolationException(
                                "Không thể lưu entity",
                                new PropertyValueException(
                                        "not-null property references a null value",
                                        "Question",
                                        propertyName)),
                        propertyName),
                Arguments.of(
                        "Lỗi integrity không xác định",
                        new DataIntegrityViolationException("Chi tiết schema nội bộ"),
                        "Chi tiết schema nội bộ"));
    }
}
