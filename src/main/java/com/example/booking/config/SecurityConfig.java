package com.example.booking.config;

import com.example.booking.security.JwtFilter;
import com.example.booking.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Resources: GET = USER+ADMIN, write ops = ADMIN only
                .requestMatchers(HttpMethod.GET, "/resources/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/resources/**").hasRole("ADMIN")

                // Reservations
                .requestMatchers(HttpMethod.POST, "/reservations").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/reservations/my").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/reservations/{id}").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/reservations").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/reservations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/reservations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/reservations/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" +
                        (authException.getMessage() != null ? authException.getMessage() : "Full authentication is required") +
                        "\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(
                        "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"" +
                        (accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access Denied") +
                        "\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}"
                    );
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
