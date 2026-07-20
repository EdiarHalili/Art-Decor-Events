package com.artdecor.workforce.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.artdecor.workforce.application.attendance.AttendanceException;
import com.artdecor.workforce.application.management.ManagementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void returnsStructuredManagementErrors() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleManagement(new ManagementException("Employee ID already exists."));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MANAGEMENT_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Employee ID already exists.");
    }

    @Test
    void doesNotExposeInternalIllegalArgumentMessages() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("rawPassword cannot be null"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("The request is invalid.");
    }

    @Test
    void returnsConflictForDuplicateAttendanceActions() {
        ResponseEntity<ApiExceptionHandler.ApiError> response =
                handler.handleAttendance(new AttendanceException(
                        "DUPLICATE_ACTIVE_CHECK_IN",
                        "Ju tashmë keni filluar orarin e punës."
                ));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Ju tashmë keni filluar orarin e punës.");
    }
}
