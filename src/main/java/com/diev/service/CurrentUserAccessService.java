package com.diev.service;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.exception.ErrorCode;
import com.diev.exception.ForbiddenException;
import com.diev.exception.NotFoundException;
import com.diev.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessService {

    private final UserRepository userRepository;

    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    public boolean isNotAdmin(UUID userId) {
        return !Role.ADMIN.name().equals(requireUser(userId).getRole());
    }

    public void requireOwnerOrAdmin(UUID actorId, UUID ownerId, ErrorCode errorCode) {
        if (!actorId.equals(ownerId) && isNotAdmin(actorId)) {
            throw new ForbiddenException(errorCode);
        }
    }
}