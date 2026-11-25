package cl.induztek.ms_usuarios.controller;

import cl.induztek.ms_usuarios.dto.LoginRequest;
import cl.induztek.ms_usuarios.dto.LoginResponse;
import cl.induztek.ms_usuarios.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
        AuthService authService
    ) {

        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
        @Valid @RequestBody LoginRequest request
    ) {

        return authService.autenticar(request);
    }
}