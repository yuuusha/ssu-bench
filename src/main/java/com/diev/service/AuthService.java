package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
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
            throw new RuntimeException("User already exists");
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
                .orElseThrow();
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getBlocked()) {
            throw new RuntimeException("User blocked");
        }

        return user;
    }
}