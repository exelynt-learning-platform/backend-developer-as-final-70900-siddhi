package com.example.booking;

import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.LoginResponse;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtUtil;
import com.example.booking.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ─── Login Tests ───────────────────────────────────────────────────────────

    @Test
    void login_shouldReturnToken_whenValidCredentials() {
        User user = new User();
        user.setEmail("admin@booking.com");
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@booking.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("admin@booking.com", "ADMIN")).thenReturn("mock-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@booking.com");
        request.setPassword("admin123");

        LoginResponse response = authService.login(request);

        assertEquals("mock-token", response.getToken());
        assertEquals("ADMIN", response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmail("wrong@email.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@email.com");
        request.setPassword("password");

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    // ─── Register Tests ────────────────────────────────────────────────────────

    @Test
    void register_shouldSaveUser_whenEmailIsNew() {
        when(userRepository.findByEmail("new@user.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@user.com");
        request.setPassword("password123");
        request.setRole(Role.USER);

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        User existing = new User();
        existing.setEmail("existing@user.com");
        when(userRepository.findByEmail("existing@user.com")).thenReturn(Optional.of(existing));

        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@user.com");
        request.setPassword("password");
        request.setRole(Role.USER);

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}