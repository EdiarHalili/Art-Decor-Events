package com.artdecor.workforce.api;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.application.management.ManagementException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", "Please check the submitted fields.", Map.of()));
    }

    @ExceptionHandler(ManagementException.class)
    public ResponseEntity<ApiError> handleManagement(ManagementException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("MANAGEMENT_ERROR", exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, Object> details) {
    }
}
