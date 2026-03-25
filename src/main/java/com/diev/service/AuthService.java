package com.diev.service;

import com.diev.api.auth.AuthResponse;
import com.diev.api.auth.AuthUserResponse;
import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.*;
import com.diev.repo.UserRepository;
import com.diev.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(String email, String password, Role role) {
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            throw new ConflictException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UUID id = UUID.randomUUID();
        String hash = passwordEncoder.encode(password);

        userRepository.create(
                id,
                email,
                hash,
                role.name(),
                0,
                false
        );

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return buildAuthResponse(user);
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getBlocked()) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                toPublicUser(user)
        );
    }

    private AuthUserResponse toPublicUser(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                Role.valueOf(user.getRole()),
                user.getBalance(),
                user.getBlocked()
        );
    }
}