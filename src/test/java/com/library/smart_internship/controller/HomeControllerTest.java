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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private HomeController controller;

    @BeforeEach
    void setUp() {
        controller = new HomeController(
                studentRepository, studentService, passwordResetTokenRepository, emailService, passwordEncoder);

        // Set up mock request context for ServletUriComponentsBuilder
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @Test
    void registerCreatesStudentAndRedirectsToLogin() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");

        String result = controller.registerSubmit("Jane Doe", "jane@example.com",
                "secret123", "STUDENT", "Java, SQL");

        assertThat(result).isEqualTo("redirect:/login?registered");

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).createStudent(captor.capture());

        Student saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashed-secret");
        assertThat(saved.getRole()).isEqualTo("STUDENT");
        assertThat(saved.getSkills()).isEqualTo("Java, SQL");
    }

    @Test
    void registerIgnoresSkillsForRecruiterRole() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        String result = controller.registerSubmit("Acme Corp", "hr@acme.com",
                "secret123", "RECRUITER", "");

        assertThat(result).isEqualTo("redirect:/login?registered");

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).createStudent(captor.capture());

        assertThat(captor.getValue().getSkills()).isNull();
    }

    @Test
    void registerRejectsDuplicateEmailWithoutCreatingStudent() {
        Student existing = new Student();
        existing.setEmail("jane@example.com");
        when(studentRepository.findAll()).thenReturn(List.of(existing));

        String result = controller.registerSubmit("Jane Doe", "JANE@example.com",
                "secret123", "STUDENT", null);

        assertThat(result).isEqualTo("redirect:/register?error");
        verify(studentService, never()).createStudent(any());
    }

    @Test
    void forgotPasswordCreatesTokenAndSendsEmailWhenStudentExists() {
        Student student = new Student();
        student.setEmail("jane@example.com");
        when(studentRepository.findAll()).thenReturn(List.of(student));

        Model model = new ExtendedModelMap();
        String result = controller.forgotPasswordSubmit("jane@example.com", model);

        assertThat(result).isEqualTo("forgot-password");
        assertThat(model.containsAttribute("message")).isTrue();

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("jane@example.com"), anyString());
    }

    @Test
    void forgotPasswordShowsSameMessageWhenStudentDoesNotExist() {
        when(studentRepository.findAll()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String result = controller.forgotPasswordSubmit("nobody@example.com", model);

        assertThat(result).isEqualTo("forgot-password");
        assertThat(model.containsAttribute("message")).isTrue();

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordGetExposesTokenToModel() {
        Model model = new ExtendedModelMap();
        String result = controller.resetPassword("abc123", model);

        assertThat(result).isEqualTo("reset-password");
        assertThat(model.getAttribute("token")).isEqualTo("abc123");
    }

    @Test
    void resetPasswordSubmitUpdatesPasswordForValidToken() {
        Student student = new Student();
        student.setEmail("jane@example.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setStudent(student);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass123")).thenReturn("hashed-newpass");

        Model model = new ExtendedModelMap();
        String result = controller.resetPasswordSubmit("valid-token", "newpass123", model);

        assertThat(result).isEqualTo("reset-password");
        assertThat(model.containsAttribute("success")).isTrue();
        assertThat(student.getPassword()).isEqualTo("hashed-newpass");

        verify(studentRepository).save(student);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void resetPasswordSubmitRejectsExpiredToken() {
        Student student = new Student();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setStudent(student);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        Model model = new ExtendedModelMap();
        String result = controller.resetPasswordSubmit("expired-token", "newpass123", model);

        assertThat(result).isEqualTo("reset-password");
        assertThat(model.containsAttribute("error")).isTrue();

        verify(studentRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordSubmitRejectsUnknownToken() {
        when(passwordResetTokenRepository.findByToken("bogus")).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();
        String result = controller.resetPasswordSubmit("bogus", "newpass123", model);

        assertThat(result).isEqualTo("reset-password");
        assertThat(model.containsAttribute("error")).isTrue();

        verify(studentRepository, never()).save(any());
    }
}