package com.multigenesys.booking.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multigenesys.booking.dto.response.ErrorResponse;
import com.multigenesys.booking.security.CustomUserDetailsService;
import com.multigenesys.booking.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler to achieve high coverage.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404")
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource", "id", 99L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400")
    void testHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid input", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle ConflictException with 409")
    void testHandleConflictException() {
        ConflictException ex = new ConflictException("Resource already exists");

        ResponseEntity<ErrorResponse> response = handler.handleConflictException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Resource already exists", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation error map")
    void testHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "username", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("must not be blank", response.getBody().getValidationErrors().get("username"));
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException with violation map")
    void testHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("email");
        when(violation.getMessage()).thenReturn("must be valid email");

        ConstraintViolationException ex = new ConstraintViolationException("Constraint violation", Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("must be valid email", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with 400")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Malformed JSON",
                new MockHttpInputMessage("{}".getBytes(StandardCharsets.UTF_8))
        );

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON Request", response.getBody().getError());
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException with 400")
    void testHandleMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getRequiredType()).thenAnswer(inv -> Long.class);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("id"));
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401")
    void testHandleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Should handle AuthenticationException with 401")
    void testHandleAuthenticationException() {
        InsufficientAuthenticationException ex = new InsufficientAuthenticationException("Not authenticated");

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not authenticated", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should re-throw AccessDeniedException")
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        assertThrows(AccessDeniedException.class, () -> handler.handleAccessDeniedException(ex));
    }

    @Test
    @DisplayName("Should handle generic Exception with 500")
    void testHandleGlobalException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
    }
}
