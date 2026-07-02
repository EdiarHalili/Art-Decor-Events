package com.artdecor.workforce.api;

import static org.assertj.core.api.Assertions.assertThat;

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
}

