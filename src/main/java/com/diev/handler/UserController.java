package com.diev.handler;

import com.diev.entity.User;
import com.diev.service.UserService;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/block")
    public void blockUser(@PathVariable UUID id) {

        userService.blockUser(id);
    }

    @PostMapping("/{id}/unblock")
    public void unblockUser(@PathVariable UUID id) {

        userService.unblockUser(id);
    }
}