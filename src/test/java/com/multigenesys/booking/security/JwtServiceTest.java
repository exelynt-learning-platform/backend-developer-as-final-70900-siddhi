package com.multigenesys.booking.security;

import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .build();

        userPrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate valid JWT token with user claims")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(userPrincipal);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("testuser", jwtService.extractUsername(token));
        assertEquals(1L, jwtService.extractUserId(token));
        assertEquals("ROLE_USER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should confirm valid token for user principal")
    void testIsTokenValid() {
        String token = jwtService.generateToken(userPrincipal);

        assertTrue(jwtService.isTokenValid(token, userPrincipal));
    }

    @Test
    @DisplayName("Should reject invalid or malformed JWT token")
    void testInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }
}
