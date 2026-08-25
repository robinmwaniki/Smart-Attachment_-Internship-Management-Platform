package com.library.smart_internship.controller;

import com.library.smart_internship.dto.AuthResponse;
import com.library.smart_internship.dto.LoginRequest;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final StudentRepository studentRepository;
    private final JwtService jwtService;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            Student student = studentRepository.findAll().stream()
                    .filter(s -> s.getEmail().equals(loginRequest.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtService.generateToken(loginRequest.getEmail(),
                    Collections.singletonList("ROLE_" + student.getRole()));

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    student.getId(),
                    student.getEmail(),
                    student.getRole()
            ));
        } catch (AuthenticationException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage("Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage("Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("message", "Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);

        if (email != null && jwtService.isTokenValid(token, email)) {
            response.put("valid", true);
            response.put("email", email);
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);
        }

        response.put("valid", false);
        response.put("message", "Invalid or expired token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}