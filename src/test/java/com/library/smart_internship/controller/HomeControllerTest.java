package com.library.smart_internship.controller;

import com.library.smart_internship.entity.PasswordResetToken;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.PasswordResetTokenRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import com.library.smart_internship.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HomeController controller = new HomeController(
                studentRepository, studentService, passwordResetTokenRepository, emailService, passwordEncoder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void registerCreatesStudentAndRedirectsToLogin() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");

        mockMvc.perform(post("/register")
                        .param("name", "Robin Mwaniki")
                        .param("email", "robin@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT")
                        .param("skills", "Java, SQL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).createStudent(captor.capture());

        Student saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("robin@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashed-secret");
        assertThat(saved.getRole()).isEqualTo("STUDENT");
        assertThat(saved.getSkills()).isEqualTo("Java, SQL");
    }

    @Test
    void registerIgnoresSkillsForRecruiterRole() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        mockMvc.perform(post("/register")
                        .param("name", "Google Corp")
                        .param("email", "hr@google.com")
                        .param("password", "secret123")
                        .param("role", "RECRUITER")
                        .param("skills", ""))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).createStudent(captor.capture());

        assertThat(captor.getValue().getSkills()).isNull();
    }

    @Test
    void registerRejectsDuplicateEmailWithoutCreatingStudent() throws Exception {
        Student existing = new Student();
        existing.setEmail("robin@example.com");
        when(studentRepository.findAll()).thenReturn(List.of(existing));

        mockMvc.perform(post("/register")
                        .param("name", "Robin Mwaniki")
                        .param("email", "ROBIN@example.com")
                        .param("password", "secret123")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?error"));

        verify(studentService, never()).createStudent(any());
    }

    @Test
    void forgotPasswordCreatesTokenAndSendsEmailWhenStudentExists() throws Exception {
        Student student = new Student();
        student.setEmail("robin@example.com");
        when(studentRepository.findAll()).thenReturn(List.of(student));

        mockMvc.perform(post("/forgot-password").param("email", "robin@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("message"));

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("robin@example.com"), anyString());
    }

    @Test
    void forgotPasswordShowsSameMessageWhenStudentDoesNotExist() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/forgot-password").param("email", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("message"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordGetExposesTokenToModel() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("token", "abc123"));
    }

    @Test
    void resetPasswordSubmitUpdatesPasswordForValidToken() throws Exception {
        Student student = new Student();
        student.setEmail("jane@example.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setStudent(student);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass123")).thenReturn("hashed-newpass");

        mockMvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "newpass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("success"));

        assertThat(student.getPassword()).isEqualTo("hashed-newpass");
        verify(studentRepository).save(student);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void resetPasswordSubmitRejectsExpiredToken() throws Exception {
        Student student = new Student();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setStudent(student);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        mockMvc.perform(post("/reset-password")
                        .param("token", "expired-token")
                        .param("password", "newpass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("error"));

        verify(studentRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordSubmitRejectsUnknownToken() throws Exception {
        when(passwordResetTokenRepository.findByToken("bogus")).thenReturn(Optional.empty());

        mockMvc.perform(post("/reset-password")
                        .param("token", "bogus")
                        .param("password", "newpass123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));

        verify(studentRepository, never()).save(any());
    }
}