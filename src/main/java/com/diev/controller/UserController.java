package com.diev.controller;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.security.CurrentUserId;
import com.diev.service.UserService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable UUID id,
                        @CurrentUserId UUID currentUserId) {
        return userService.getUser(id, currentUserId);
    }

    @GetMapping
    public List<User> getAllUsers(
            @CurrentUserId UUID currentUserId,
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {
        return userService.getAllUsers(currentUserId, limit, offset);
    }

    @PostMapping
    public User createUser(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Size(min = 8, max = 72) String password,
            @RequestParam @NotNull Role role
    ) {
        return userService.createUser(email, password, role);
    }

    @PutMapping("/{id}")
    public User updateUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role,
            @RequestParam @PositiveOrZero long balance
    ) {
        return userService.updateUser(id, currentUserId, email, password, role, balance);
    }

    @PostMapping("/{id}/balance")
    public User updateUserBalance(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId,
            @RequestParam @Positive long balance
    ) {
        return userService.updateUserBalance(id, currentUserId, balance);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        userService.deleteUser(id, currentUserId);
    }

    @PostMapping("/{id}/block")
    public void blockUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        userService.blockUser(id, currentUserId);
    }

    @PostMapping("/{id}/unblock")
    public void unblockUser(
            @PathVariable UUID id,
            @CurrentUserId UUID currentUserId
    ) {
        userService.unblockUser(id, currentUserId);
    }
}