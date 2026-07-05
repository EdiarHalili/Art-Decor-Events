package com.artdecor.workforce.api;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.application.attendance.AttendanceException;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowException;
import com.artdecor.workforce.application.management.ManagementException;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> fields = new LinkedHashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparingInt(this::validationPriority))
                .forEach(error -> fields.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", "Please check the submitted fields.", fields));
    }

    @ExceptionHandler(ManagementException.class)
    public ResponseEntity<ApiError> handleManagement(ManagementException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("MANAGEMENT_ERROR", safeClientMessage(exception.getMessage(), "The employee request could not be completed."), Map.of()));
    }

    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<ApiError> handleAttendance(AttendanceException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(exception.code(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(DailyCheckInWindowException.class)
    public ResponseEntity<ApiError> handleDailyWindow(DailyCheckInWindowException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("DAILY_CHECK_IN_WINDOW_ERROR", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", safeClientMessage(exception.getMessage(), "The request is invalid."), Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST_BODY", "The request body is invalid.", Map.of()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, Object> fields = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage() == null ? "Invalid value." : violation.getMessage(),
                        (first, ignored) -> first
                ));

        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", "Please check the submitted fields.", fields));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DATA_CONFLICT", "This record conflicts with existing data.", Map.of()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException exception) {
        log.error("Unhandled API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Something went wrong. Please try again.", Map.of()));
    }

    private String safeClientMessage(String message, String fallback) {
        if (message == null || message.isBlank() || looksInternal(message)) {
            return fallback;
        }
        return message;
    }

    private boolean looksInternal(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("rawpassword")
                || normalized.contains("nullpointerexception")
                || normalized.contains("illegalargumentexception")
                || normalized.contains("constraintviolationexception")
                || normalized.contains("stack trace")
                || normalized.contains("cannot be null");
    }

    private int validationPriority(FieldError error) {
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code)) {
            return 0;
        }
        return 1;
    }

    public record ApiError(String code, String message, Map<String, Object> details) {
    }
}
