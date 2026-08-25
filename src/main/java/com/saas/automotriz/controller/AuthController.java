package com.saas.automotriz.controller;

import com.saas.automotriz.request.LoginRequest;
import com.saas.automotriz.request.RegisterRequest;
import com.saas.automotriz.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.saas.automotriz.model.User; // ✅ tu User con getId()
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

import com.saas.automotriz.dto.*;
import com.saas.automotriz.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

//@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("🔐 Intentando login con: " + request.getEmail());
            System.out.println("🔑 Password recibido: " + request.getPassword());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            System.out.println("👤 Usuario encontrado: " + userDetails.getUsername());
            System.out.println("🔒 Hash en BD: " + userDetails.getPassword());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            String token = jwtUtils.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(token));

        } catch (BadCredentialsException e) {
            System.out.println("❌ Credenciales incorrectas");
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Usuario registrado con éxito",
                    "userId", user.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
