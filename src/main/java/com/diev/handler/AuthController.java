package com.diev.handler;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Role role
    ) {

        return authService.register(email, password, role);
    }

    @PostMapping("/login")
    public User login(
            @RequestParam String email,
            @RequestParam String password
    ) {

        return authService.login(email, password);
    }
}