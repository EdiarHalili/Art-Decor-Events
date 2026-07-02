package com.artdecor.workforce.api;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.application.attendance.AttendanceException;
import com.artdecor.workforce.application.management.ManagementException;
import java.util.stream.Collectors;
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
        Map<String, Object> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage(),
                        (first, ignored) -> first
                ));

        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", "Please check the submitted fields.", fields));
    }

    @ExceptionHandler(ManagementException.class)
    public ResponseEntity<ApiError> handleManagement(ManagementException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("MANAGEMENT_ERROR", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<ApiError> handleAttendance(AttendanceException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(exception.code(), exception.getMessage(), Map.of()));
    }

    public record ApiError(String code, String message, Map<String, Object> details) {
    }
}
