package com.sahilpanmand.tatkal.service;

import com.sahilpanmand.tatkal.dto.LoginRequest;
import com.sahilpanmand.tatkal.dto.RegisterRequest;
import com.sahilpanmand.tatkal.entity.Role;
import com.sahilpanmand.tatkal.entity.User;
import com.sahilpanmand.tatkal.exception.ConflictException;
import com.sahilpanmand.tatkal.exception.UnauthorizedException;
import com.sahilpanmand.tatkal.repository.UserRepository;
import com.sahilpanmand.tatkal.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret-123456", 60);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, encoder, jwtUtil);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("sahil@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Sahil", "sahil@test.com", "secret1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginReturnsTokenForCorrectPasswordAndRejectsWrongOne() {
        User user = new User("Sahil", "sahil@test.com", encoder.encode("secret1"), Role.USER);
        when(userRepository.findByEmail("sahil@test.com")).thenReturn(Optional.of(user));

        var response = authService.login(new LoginRequest("sahil@test.com", "secret1"));
        assertThat(jwtUtil.validate(response.token()).getSubject()).isEqualTo("sahil@test.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("sahil@test.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
