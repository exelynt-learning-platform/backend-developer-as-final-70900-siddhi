package com.multigenesys.booking.service;

import com.multigenesys.booking.dto.request.LoginRequest;
import com.multigenesys.booking.dto.request.RegisterRequest;
import com.multigenesys.booking.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse register(RegisterRequest registerRequest);
}
