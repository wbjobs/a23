package com.research.backend.service;

import com.research.backend.dto.JwtResponse;
import com.research.backend.dto.LoginRequest;
import com.research.backend.dto.RegisterRequest;
import com.research.backend.entity.User;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    User register(RegisterRequest registerRequest);
}
