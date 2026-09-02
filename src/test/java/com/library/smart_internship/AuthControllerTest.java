package com.library.smart_internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.smart_internship.dto.LoginRequest;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authenticationManager, studentRepository, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@example.com");
        student.setRole("STUDENT");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "student@example.com", "password");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(jwtService.generateToken("student@example.com", List.of("ROLE_STUDENT")))
                .thenReturn("fake-jwt-token");

        LoginRequest request = new LoginRequest("student@example.com", "password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void loginReturnsUnauthorizedForBadCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("student@example.com", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void validateReturnsUnauthorizedWithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateReturnsValidForGoodToken() throws Exception {
        when(jwtService.extractEmail("good-token")).thenReturn("student@example.com");
        when(jwtService.isTokenValid("good-token", "student@example.com")).thenReturn(true);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("student@example.com"));
    }

    @Test
    void validateReturnsUnauthorizedForExpiredToken() throws Exception {
        when(jwtService.extractEmail("bad-token")).thenReturn("student@example.com");
        when(jwtService.isTokenValid("bad-token", "student@example.com")).thenReturn(false);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}