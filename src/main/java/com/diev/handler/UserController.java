package com.diev.handler;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.service.UserService;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable UUID id) {

        return userService.getUser(id);
    }

    @GetMapping
    public List<User> getAllUsers(
            @RequestParam(defaultValue = "20") @Positive int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero int offset
    ) {

        return userService.getAllUsers(limit, offset);
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
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role,
            @RequestParam @PositiveOrZero long balance
    ) {
        return userService.updateUser(id, email, password, role, balance);
    }

    @PostMapping("/{id}/balance")
    public User updateUserBalance(
            @PathVariable UUID id,
            @RequestParam @Positive long balance
    ) {
        return userService.updateUserBalance(id, balance);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {

        userService.deleteUser(id);
    }

    @PostMapping("/{id}/block")
    public void blockUser(@PathVariable UUID id) {

        userService.blockUser(id);
    }

    @PostMapping("/{id}/unblock")
    public void unblockUser(@PathVariable UUID id) {

        userService.unblockUser(id);
    }
}