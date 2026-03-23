package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.BadRequestException;
import com.diev.exception.ConflictException;
import com.diev.exception.NotFoundException;
import com.diev.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User createUser(String email, String password, Role role) {

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

    public User updateUser(UUID id, String email, String password, Role role, long balance) {

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        String hash = passwordEncoder.encode(password);

        userRepository.update(
                id,
                email,
                hash,
                role.name(),
                balance
        );

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }
    public User updateUserBalance(UUID id, long balance) {

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        if (balance <= 0) {
            throw new BadRequestException("INVALID_BALANCE", "Balance must be greater than zero.");
        }

        userRepository.updateBalance(id, balance);

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    public void deleteUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.delete(id);
    }

    public User getUser(UUID id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    public List<User> getAllUsers(int limit, int offset) {
        return userRepository.findAll(limit, offset);
    }

    public void blockUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.block(id);
    }

    public void unblockUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.unblock(id);
    }
}