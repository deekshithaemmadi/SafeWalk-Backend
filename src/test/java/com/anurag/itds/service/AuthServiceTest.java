package com.anurag.itds.service;

import com.anurag.itds.dto.AuthResponse;
import com.anurag.itds.dto.LoginRequest;
import com.anurag.itds.dto.RegisterRequest;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.ConflictException;
import com.anurag.itds.exception.UnauthorizedException;
import com.anurag.itds.repository.EmergencyContactRepository;
import com.anurag.itds.repository.UserRepository;
import com.anurag.itds.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmergencyContactRepository contactRepository = mock(EmergencyContactRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService("unit-test-secret-unit-test-secret-0123456789", 60_000);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, contactRepository, encoder, jwtService);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 7L);
            return u;
        });
    }

    @Test
    void registerNormalisesEmailHashesPasswordAndIssuesToken() {
        AuthResponse response = authService.register(
                new RegisterRequest(" Asha ", " Asha@Example.COM ", "9876543210", "secret123", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("asha@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Asha");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(jwtService.parseToken(response.token()).getSubject()).isEqualTo("7");
    }

    @Test
    void registerDuplicateEmailIsConflictAndSavesNothing() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Dup", "dup@example.com", "9876543210", "secret123", List.of())))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailGivesSameGenericError() {
        User user = new User("U", "u@example.com", "9876543210", encoder.encode("right-pass"));
        ReflectionTestUtils.setField(user, "id", 3L);
        when(userRepository.findByEmailIgnoreCase("u@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("u@example.com", "wrong-pass")))
                .isInstanceOf(UnauthorizedException.class).hasMessage("Invalid email or password.");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "x")))
                .isInstanceOf(UnauthorizedException.class).hasMessage("Invalid email or password.");
    }

    @Test
    void loginIsCaseInsensitiveOnEmail() {
        User user = new User("U", "u@example.com", "9876543210", encoder.encode("right-pass"));
        ReflectionTestUtils.setField(user, "id", 3L);
        when(userRepository.findByEmailIgnoreCase("u@example.com")).thenReturn(Optional.of(user));
        assertThat(authService.login(new LoginRequest("U@Example.com", "right-pass")).token()).isNotBlank();
    }
}
