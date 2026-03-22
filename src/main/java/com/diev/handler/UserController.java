package com.diev.handler;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
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
    public List<User> getAllUsers() {

        return userService.getAllUsers();
    }

    @PostMapping
    public User createUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role
    ) {
        return userService.createUser(email, password, role);
    }

    @PutMapping("/{id}")
    public User updateUser(
            @PathVariable UUID id,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam long balance
    ) {
        return userService.updateUser(id, email, password, role, balance);
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