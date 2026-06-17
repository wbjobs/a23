package com.research.backend.service.impl;

import com.research.backend.entity.User;
import com.research.backend.repository.UserRepository;
import com.research.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        User existing = getUserById(user.getId());
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }
        existing.setName(user.getName());
        existing.setAffiliation(user.getAffiliation());
        existing.setUpdateTime(LocalDateTime.now());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(user.getPassword());
        }
        if (user.getEnabled() != null) {
            existing.setEnabled(user.getEnabled());
        }
        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
