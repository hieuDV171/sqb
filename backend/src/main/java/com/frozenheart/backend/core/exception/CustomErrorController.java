package com.frozenheart.backend.core.exception;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.GlobalResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<GlobalResponse<Void>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");

        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());
                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(GlobalResponse.error(ResponseCode.RESOURCE_NOT_FOUND));
                } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                    return ResponseEntity
                            .status(HttpStatus.METHOD_NOT_ALLOWED)
                            .body(GlobalResponse.error(ResponseCode.METHOD_NOT_ALLOWED));
                } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                    return ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(GlobalResponse.error(ResponseCode.TOKEN_INVALID_OR_EXPIRED));
                } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(GlobalResponse.error(ResponseCode.ACCESS_DENIED));
                }
            } catch (NumberFormatException _) {
                // Ignore parse failure and fall through
            }
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.error(ResponseCode.UNHANDLED_EXCEPTION));
    }
}
