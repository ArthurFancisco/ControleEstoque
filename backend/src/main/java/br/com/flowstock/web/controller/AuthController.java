package br.com.flowstock.web.controller;

import br.com.flowstock.service.AuthService;
import br.com.flowstock.web.dto.AuthDtos.LoginRequest;
import br.com.flowstock.web.dto.AuthDtos.LoginResponse;
import br.com.flowstock.web.dto.AuthDtos.UserMeResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserMeResponse me() {
        return authService.me();
    }
}
