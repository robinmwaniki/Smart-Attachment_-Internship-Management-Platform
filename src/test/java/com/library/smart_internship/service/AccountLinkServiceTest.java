package com.library.smart_internship.service;

import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLinkServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountLinkService accountLinkService;

    @Test
    void linksExistingStudentOnlyAfterVerifiedEmailAndPasswordProof() {
        Student student = student("STUDENT");
        when(studentRepository.findAllByEmailIgnoreCase("student@example.com")).thenReturn(List.of(student));
        when(passwordEncoder.matches("existing-password", "stored-hash")).thenReturn(true);
        when(studentRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());
        when(studentRepository.save(student)).thenReturn(student);

        Student linked = accountLinkService.linkExistingAccount(jwt(true, List.of("STUDENT")), "existing-password");

        assertEquals("keycloak-subject", linked.getKeycloakSubject());
        verify(studentRepository).save(student);
    }

    @Test
    void rejectsUnverifiedEmail() {
        assertThrows(AccountLinkService.AccountLinkException.class,
                () -> accountLinkService.linkExistingAccount(jwt(false, List.of("STUDENT")), "existing-password"));
        verifyNoInteractions(studentRepository, passwordEncoder);
    }

    @Test
    void rejectsIncorrectExistingPassword() {
        Student student = student("STUDENT");
        when(studentRepository.findAllByEmailIgnoreCase("student@example.com")).thenReturn(List.of(student));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThrows(AccountLinkService.AccountLinkException.class,
                () -> accountLinkService.linkExistingAccount(jwt(true, List.of("STUDENT")), "wrong-password"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void rejectsRoleMismatch() {
        Student student = student("STUDENT");
        when(studentRepository.findAllByEmailIgnoreCase("student@example.com")).thenReturn(List.of(student));
        when(passwordEncoder.matches("existing-password", "stored-hash")).thenReturn(true);
        when(studentRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());

        assertThrows(AccountLinkService.AccountLinkException.class,
                () -> accountLinkService.linkExistingAccount(jwt(true, List.of("COMPANY")), "existing-password"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void mapsLegacyRecruiterRoleToCompanyRealmRole() {
        Student recruiter = student("RECRUITER");
        when(studentRepository.findAllByEmailIgnoreCase("student@example.com")).thenReturn(List.of(recruiter));
        when(passwordEncoder.matches("existing-password", "stored-hash")).thenReturn(true);
        when(studentRepository.findByKeycloakSubject("keycloak-subject")).thenReturn(Optional.empty());
        when(studentRepository.save(recruiter)).thenReturn(recruiter);

        Student linked = accountLinkService.linkExistingAccount(jwt(true, List.of("COMPANY")), "existing-password");

        assertEquals("keycloak-subject", linked.getKeycloakSubject());
        assertEquals("RECRUITER", linked.getRole());
    }

    @Test
    void rejectsAmbiguousDuplicateEmailProfiles() {
        when(studentRepository.findAllByEmailIgnoreCase("student@example.com"))
                .thenReturn(List.of(student("STUDENT"), student("STUDENT")));

        assertThrows(AccountLinkService.AccountLinkException.class,
                () -> accountLinkService.linkExistingAccount(jwt(true, List.of("STUDENT")), "existing-password"));
        verifyNoInteractions(passwordEncoder);
    }

    private Student student(String role) {
        Student student = new Student();
        student.setId(17L);
        student.setEmail("student@example.com");
        student.setPassword("stored-hash");
        student.setRole(role);
        return student;
    }

    private Jwt jwt(boolean emailVerified, List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("keycloak-subject")
                .claim("email", "student@example.com")
                .claim("email_verified", emailVerified)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
