package com.sindibad.App.controller;

import com.sindibad.App.dto.AuthResponseDTO;
import com.sindibad.App.dto.LoginRequestDTO;
import com.sindibad.App.dto.RegisterGuideRequestDTO;
import com.sindibad.App.dto.RegisterRequestDTO;
import com.sindibad.App.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==================== Register Tourist ====================

    @Operation(summary = "Register a new Tourist", description = "Creates a new tourist account and returns a JWT token.")
    @PostMapping("/register/tourist")
    public ResponseEntity<AuthResponseDTO> registerTourist(
            @Valid @RequestBody RegisterRequestDTO registerDTO) {

        AuthResponseDTO response =
                authService.registerTourist(registerDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==================== Register Guide ====================

    @Operation(summary = "Register a new Tour Guide", description = "Creates a new guide application pending admin approval.")
    @PostMapping("/register/guide")
    public ResponseEntity<AuthResponseDTO> registerGuide(
            @Valid @RequestBody RegisterGuideRequestDTO guideDTO) {

        AuthResponseDTO response =
                authService.registerGuide(guideDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==================== Login ====================

    @Operation(summary = "User Login", description = "Authenticates Admin, Guide, or Tourist and returns JWT with role.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginDTO) {

        AuthResponseDTO response =
                authService.login(loginDTO);

        return ResponseEntity.ok(response);
    }
}