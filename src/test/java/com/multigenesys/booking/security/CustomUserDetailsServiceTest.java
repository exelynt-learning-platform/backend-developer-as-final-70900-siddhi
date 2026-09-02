package com.multigenesys.booking.security;

import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername should find user by username")
    void testLoadByUsername() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(sampleUser));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername("testuser");

        assertNotNull(principal);
        assertEquals("testuser", principal.getUsername());
        assertEquals("test@example.com", principal.getEmail());
        assertEquals(Role.ROLE_USER, principal.getRole());
    }

    @Test
    @DisplayName("loadUserByUsername should find user by email")
    void testLoadByEmail() {
        when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
                .thenReturn(Optional.of(sampleUser));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(principal);
        assertEquals("testuser", principal.getUsername());
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException for unknown user")
    void testLoadByUsernameNotFound() {
        when(userRepository.findByUsernameOrEmail("unknown", "unknown"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, 
                () -> userDetailsService.loadUserByUsername("unknown"));
    }

    @Test
    @DisplayName("UserPrincipal should return correct authorities")
    void testUserPrincipalAuthorities() {
        UserPrincipal principal = UserPrincipal.create(sampleUser);

        assertNotNull(principal.getAuthorities());
        assertEquals(1, principal.getAuthorities().size());
        assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    @DisplayName("UserPrincipal with ADMIN role should have ROLE_ADMIN authority")
    void testAdminPrincipalAuthorities() {
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .password("encoded")
                .role(Role.ROLE_ADMIN)
                .build();

        UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);

        assertEquals("ROLE_ADMIN", adminPrincipal.getAuthorities().iterator().next().getAuthority());
        assertEquals(Role.ROLE_ADMIN, adminPrincipal.getRole());
    }

    @Test
    @DisplayName("loadUserById should return UserPrincipal for valid user ID")
    void testLoadUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserById(1L);

        assertNotNull(principal);
        assertEquals(1L, principal.getId());
        assertEquals("testuser", principal.getUsername());
    }

    @Test
    @DisplayName("loadUserById should throw UsernameNotFoundException for unknown ID")
    void testLoadUserByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userDetailsService.loadUserById(99L));
    }
}
