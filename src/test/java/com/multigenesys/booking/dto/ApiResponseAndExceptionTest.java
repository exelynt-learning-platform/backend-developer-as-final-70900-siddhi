package com.multigenesys.booking.dto;

import com.multigenesys.booking.dto.response.ApiResponse;
import com.multigenesys.booking.dto.response.ErrorResponse;
import com.multigenesys.booking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseAndExceptionTest {

    @Test
    @DisplayName("ApiResponse.success(data) should populate success=true and data")
    void testApiResponseSuccessData() {
        ApiResponse<String> response = ApiResponse.success("hello");
        assertTrue(response.isSuccess());
        assertEquals("hello", response.getData());
        assertNotNull(response.getTimestamp());
        assertNull(response.getMessage());
    }

    @Test
    @DisplayName("ApiResponse.success(message, data) should populate message and data")
    void testApiResponseSuccessMessageData() {
        ApiResponse<Integer> response = ApiResponse.success("created", 42);
        assertTrue(response.isSuccess());
        assertEquals("created", response.getMessage());
        assertEquals(42, response.getData());
    }

    @Test
    @DisplayName("ApiResponse.message(msg) should populate message only")
    void testApiResponseMessage() {
        ApiResponse<Void> response = ApiResponse.message("ok");
        assertTrue(response.isSuccess());
        assertEquals("ok", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("ResourceNotFoundException should format message correctly")
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource", "id", 42L);
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("Resource"));
        assertTrue(msg.contains("id"));
        assertTrue(msg.contains("42"));
    }

    @Test
    @DisplayName("ErrorResponse builder should set all fields")
    void testErrorResponseBuilder() {
        ErrorResponse response = ErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .message("Resource missing")
                .path("/api/test")
                .build();

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Resource missing", response.getMessage());
        assertEquals("/api/test", response.getPath());
    }
}
