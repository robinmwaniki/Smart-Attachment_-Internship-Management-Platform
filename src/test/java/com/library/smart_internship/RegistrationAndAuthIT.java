package com.library.smart_internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.smart_internship.dto.LoginRequest;
import com.library.smart_internship.entity.PasswordResetToken;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.PasswordResetTokenRepository;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationAndAuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registeringAStudentPersistsItToTheDatabase() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Jane Doe")
                        .param("email", "jane.integration@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT")
                        .param("skills", "Java, Spring Boot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        List<Student> matches = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals("jane.integration@example.com"))
                .toList();

        assertThat(matches).hasSize(1);
        Student saved = matches.get(0);
        assertThat(saved.getRole()).isEqualTo("STUDENT");
        assertThat(saved.getSkills()).isEqualTo("Java, Spring Boot");
        assertThat(saved.getPassword()).isNotEqualTo("secret123");
    }

    @Test
    void registeringWithADuplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "First Account")
                        .param("email", "duplicate.integration@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/register")
                        .param("name", "Second Account")
                        .param("email", "duplicate.integration@example.com")
                        .param("password", "different456")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?error"));

        long count = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals("duplicate.integration@example.com"))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void registeredStudentCanLoginAndValidateToken() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Login Test")
                        .param("email", "login.integration@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection());

        LoginRequest loginRequest = new LoginRequest("login.integration@example.com", "secret123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("login.integration@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/auth/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("login.integration@example.com"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Wrong Password Test")
                        .param("email", "wrongpass.integration@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection());

        LoginRequest loginRequest = new LoginRequest("wrongpass.integration@example.com", "not-the-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullPasswordResetFlowAllowsLoginWithNewPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Reset Flow Test")
                        .param("email", "reset.integration@example.com")
                        .param("password", "oldpassword1")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/forgot-password").param("email", "reset.integration@example.com"))
                .andExpect(status().isOk());

        Student student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals("reset.integration@example.com"))
                .findFirst()
                .orElseThrow();

        Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getStudent().getId().equals(student.getId()))
                .findFirst();

        assertThat(resetToken).isPresent();

        mockMvc.perform(post("/reset-password")
                        .param("token", resetToken.get().getToken())
                        .param("password", "newpassword2"))
                .andExpect(status().isOk());

        LoginRequest oldPasswordLogin = new LoginRequest("reset.integration@example.com", "oldpassword1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldPasswordLogin)))
                .andExpect(status().isUnauthorized());

        LoginRequest newPasswordLogin = new LoginRequest("reset.integration@example.com", "newpassword2");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPasswordLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}