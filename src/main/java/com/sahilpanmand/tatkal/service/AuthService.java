package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.AuthResponse;
import com.sahilpanmand.tatkal.dto.LoginRequest;
import com.sahilpanmand.tatkal.dto.RegisterRequest;
import com.sahilpanmand.tatkal.entity.User;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.exception.UnauthorizedException;
import com.sahilpanmand.tatkal.repository.UserRepository;
import com.sahilpanmand.tatkal.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(
                new User(request.name(), email, passwordEncoder.encode(request.password())));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }
}
