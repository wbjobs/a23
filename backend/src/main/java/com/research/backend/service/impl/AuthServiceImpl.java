package com.research.backend.service.impl;

import com.research.backend.dto.JwtResponse;
import com.research.backend.dto.LoginRequest;
import com.research.backend.dto.RegisterRequest;
import com.research.backend.entity.User;
import com.research.backend.repository.UserRepository;
import com.research.backend.service.AuthService;
import com.research.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        JwtResponse.UserInfo userInfo = JwtResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .affiliation(user.getAffiliation())
                .createTime(user.getCreateTime())
                .enabled(user.getEnabled())
                .build();

        return JwtResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(userInfo)
                .build();
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        User.UserRole userRole;
        try {
            userRole = User.UserRole.valueOf(registerRequest.getRole());
        } catch (Exception e) {
            userRole = User.UserRole.ROLE_AUTHOR;
        }
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role(userRole)
                .name(registerRequest.getName())
                .affiliation(registerRequest.getAffiliation())
                .enabled(true)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }
}
