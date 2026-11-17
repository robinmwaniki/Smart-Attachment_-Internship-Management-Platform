package com.library.smart_internship.service;

import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountLinkService {

    private static final Set<String> SUPPORTED_ROLES =
            Set.of("STUDENT", "COMPANY", "SUPERVISOR", "ADMIN");

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Student linkExistingAccount(Jwt jwt, String existingPassword) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaim("email_verified");

        if (subject == null || subject.isBlank()
                || email == null || email.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            throw new AccountLinkException("A Keycloak account with a verified email is required.");
        }

        List<Student> matches = studentRepository.findAllByEmailIgnoreCase(email);
        if (matches.size() != 1) {
            throw new AccountLinkException("No unique existing account was found for this verified email.");
        }

        Student student = matches.getFirst();
        if (student.getPassword() == null
                || !passwordEncoder.matches(existingPassword, student.getPassword())) {
            throw new AccountLinkException("The existing account password is incorrect.");
        }

        Student subjectOwner = studentRepository.findByKeycloakSubject(subject).orElse(null);
        if (subjectOwner != null && !subjectOwner.getId().equals(student.getId())) {
            throw new AccountLinkException("This Keycloak identity is already linked to another account.");
        }

        if (student.getKeycloakSubject() != null
                && !student.getKeycloakSubject().equals(subject)) {
            throw new AccountLinkException("This application account is already linked to another Keycloak identity.");
        }

        String expectedRole = normalizeRole(student.getRole());
        if (expectedRole == null || !tokenHasRole(jwt, expectedRole)) {
            throw new AccountLinkException("The Keycloak account role does not match the existing application account.");
        }

        student.setKeycloakSubject(subject);
        return studentRepository.save(student);
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        String normalized = role.trim().toUpperCase();
        if ("RECRUITER".equals(normalized)) return "COMPANY";
        return SUPPORTED_ROLES.contains(normalized) ? normalized : null;
    }

    private boolean tokenHasRole(Jwt jwt, String expectedRole) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return false;
        Object rawRoles = realmAccess.get("roles");
        return rawRoles instanceof List<?> roles && roles.contains(expectedRole);
    }

    public static class AccountLinkException extends RuntimeException {
        public AccountLinkException(String message) {
            super(message);
        }
    }
}
