package com.diev.service;

import com.diev.api.auth.AuthResponse;
import com.diev.api.auth.AuthUserResponse;
import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.repo.UserRepository;
import com.diev.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesUserWithEncodedPassword() {
        String email = "test@example.com";
        String rawPassword = "secret123";
        Role role = Role.CUSTOMER;

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.of(new User(id, email, "encoded", role.name(), 0, false));
        });

        AuthResponse authResponse = authService.register(email, rawPassword, role);
        AuthUserResponse user = authResponse.user();

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).create(any(UUID.class), eq(email), passwordCaptor.capture(), eq(role.name()), eq(0L), eq(false));

        assertNotNull(user);
        assertEquals(email, user.email());
        assertEquals(role.name(), user.role().name());
        assertTrue(new BCryptPasswordEncoder().matches(rawPassword, passwordCaptor.getValue()));
    }

    @Test
    void registerThrowsWhenUserAlreadyExists() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User(UUID.randomUUID(), "test@example.com", "hash", "CUSTOMER", 0, false)));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> authService.register("test@example.com", "secret123", Role.CUSTOMER));

        assertEquals("User with this email already exists.", ex.getMessage());
    }

    @Test
    void loginReturnsUserWhenPasswordIsValid() {
        String email = "test@example.com";
        String rawPassword = "secret123";
        String hash = new BCryptPasswordEncoder().encode(rawPassword);
        User user = new User(UUID.randomUUID(), email, hash, "CUSTOMER", 10, false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");

        AuthResponse result = authService.login(email, rawPassword);

        assertNotNull(result);
        assertNotNull(result.user());
        assertEquals(email, result.user().email());
        assertEquals(Role.CUSTOMER, result.user().role());
        assertEquals(10, result.user().balance());
        assertFalse(result.user().blocked());
    }

    @Test
    void loginThrowsWhenUserIsBlocked() {
        String email = "test@example.com";
        String rawPassword = "secret123";
        String hash = new BCryptPasswordEncoder().encode(rawPassword);
        User user = new User(UUID.randomUUID(), email, hash, "CUSTOMER", 10, true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> authService.login(email, rawPassword));

        assertEquals("User is blocked.", ex.getMessage());
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        var ex = assertThrows(com.diev.exception.NotFoundException.class,
                () -> authService.login(email, "password"));

        assertEquals("User not found.", ex.getMessage());
    }

    @Test
    void loginThrowsWhenPasswordInvalid() {
        String email = "test@example.com";
        String rawPassword = "secret123";
        String wrongPassword = "wrong";
        String hash = new BCryptPasswordEncoder().encode(rawPassword);

        User user = new User(UUID.randomUUID(), email, hash, "CUSTOMER", 10, false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        var ex = assertThrows(com.diev.exception.UnauthorizedException.class,
                () -> authService.login(email, wrongPassword));

        assertEquals("Invalid credentials.", ex.getMessage());
    }
}