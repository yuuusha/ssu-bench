package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.BadRequestException;
import com.diev.exception.ConflictException;
import com.diev.exception.NotFoundException;
import com.diev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserAccessService accessService;

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

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(UUID id, UUID currentUserId, String email, String password, Role role, long balance) {
        accessService.requireOwnerOrAdmin(
                currentUserId,
                id,
                "ONLY_OWNER_OR_ADMIN_CAN_UPDATE_USER",
                "Only owner or admin can update this user."
        );

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

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserBalance(UUID id, UUID currentUserId, long balance) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        if (balance <= 0) {
            throw new BadRequestException("INVALID_BALANCE", "Balance must be greater than zero.");
        }

        userRepository.updateBalance(id, balance);

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public void deleteUser(UUID id, UUID currentUserId) {
        accessService.requireOwnerOrAdmin(
                currentUserId,
                id,
                "ONLY_OWNER_CAN_DELETE",
                "Only owner can delete."
        );

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.delete(id);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public User getUser(UUID id, UUID currentUserId) {
        accessService.requireOwnerOrAdmin(
                currentUserId,
                id,
                "ONLY_OWNER_OR_ADMIN_CAN_VIEW_USER",
                "Only owner or admin can view this user."
        );

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers(UUID currentUserId, int limit, int offset) {
        return userRepository.findAll(limit, offset);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void blockUser(UUID id, UUID currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.block(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void unblockUser(UUID id, UUID currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        userRepository.unblock(id);
    }
}