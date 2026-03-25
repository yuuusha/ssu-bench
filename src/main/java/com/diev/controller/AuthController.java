package com.diev.controller;

import com.diev.api.auth.AuthResponse;
import com.diev.entity.Role;
import com.diev.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password,
            @RequestParam @NotNull Role role
    ) {
        return authService.register(email, password, role);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String password
    ) {
        return authService.login(email, password);
    }
}