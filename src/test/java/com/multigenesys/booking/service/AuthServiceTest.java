package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.LoginRequest;
import com.multigenesys.booking.dto.request.RegisterRequest;
import com.multigenesys.booking.dto.response.AuthResponse;
import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.exception.ConflictException;
import com.multigenesys.booking.repository.UserRepository;
import com.multigenesys.booking.security.JwtService;
import com.multigenesys.booking.security.UserPrincipal;
import com.multigenesys.booking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    private User sampleUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, userRepository, passwordEncoder, jwtService, 3600000L);

        sampleUser = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("encoded_pass")
                .fullName("John Doe")
                .role(Role.ROLE_USER)
                .build();

        userPrincipal = UserPrincipal.create(sampleUser);
    }

    @Test
    @DisplayName("Login should return valid AuthResponse on valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("john", "password123");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(jwtService.generateToken(userPrincipal)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("john", response.getUsername());
        assertEquals("ROLE_USER", response.getRole());
    }

    @Test
    @DisplayName("Login should throw BadCredentialsException on invalid credentials")
    void testLoginBadCredentials() {
        LoginRequest request = new LoginRequest("john", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Register should create user and return AuthResponse")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("pass123")
                .fullName("New User")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("new.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("new.jwt.token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register should throw ConflictException if username is already taken")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john")
                .email("john_new@example.com")
                .password("pass123")
                .fullName("John New")
                .build();

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}
