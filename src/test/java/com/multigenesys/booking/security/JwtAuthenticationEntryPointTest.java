package com.multigenesys.booking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class SecurityHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    private final CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("JwtAuthenticationEntryPoint.commence should return 401 JSON response")
    void testEntryPointReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/reservations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new InsufficientAuthenticationException("Unauthorized");

        entryPoint.commence(request, response, ex);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("401"));
        assertTrue(body.contains("Unauthorized"));
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint.commence with null message should use default")
    void testEntryPointNullMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new InsufficientAuthenticationException(null);

        entryPoint.commence(request, response, ex);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Full authentication is required"));
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler.handle should return 403 JSON response")
    void testAccessDeniedHandlerReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        accessDeniedHandler.handle(request, response, ex);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("403"));
        assertTrue(body.contains("Forbidden"));
    }
}
