package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessService {

    private final UserRepository userRepository;

    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    public boolean isAdmin(UUID userId) {
        return Role.ADMIN.name().equals(requireUser(userId).getRole());
    }

    public void requireOwnerOrAdmin(UUID actorId, UUID ownerId, String code, String message) {
        if (!actorId.equals(ownerId) && !isAdmin(actorId)) {
            throw new ForbiddenException(code, message);
        }
    }
}