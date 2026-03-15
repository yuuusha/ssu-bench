package com.diev.service;

import com.diev.entity.User;
import com.diev.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(UUID id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void blockUser(UUID id) {

        userRepository.block(id);
    }

    public void unblockUser(UUID id) {

        userRepository.unblock(id);
    }
}