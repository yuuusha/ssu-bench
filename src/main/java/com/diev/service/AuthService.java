package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.ConflictException;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.exception.UnauthorizedException;
import com.diev.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User register(String email, String password, Role role) {

        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            throw new ConflictException("USER_ALREADY_EXISTS", "User with this email already exists.");
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

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials.");
        }

        if (user.getBlocked()) {
            throw new ForbiddenException("USER_BLOCKED", "User is blocked.");
        }

        return user;
    }
}