package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.JwtResponse;
import com.research.backend.dto.LoginRequest;
import com.research.backend.dto.RegisterRequest;
import com.research.backend.entity.User;
import com.research.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.login(loginRequest);
        return Result.success("登录成功", response);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest);
        user.setPassword(null);
        return Result.success("注册成功", user);
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
