package com.diev.handler;

import com.diev.entity.Role;
import com.diev.entity.User;
import com.diev.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role
    ) {

        return authService.register(email, password, role);
    }

    @PostMapping("/login")
    public User login(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password
    ) {

        return authService.login(email, password);
    }
}