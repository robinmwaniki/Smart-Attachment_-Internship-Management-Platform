package com.library.smart_internship.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    /**
     * Retained temporarily for the legacy Thymeleaf session-login flow.
     * Keycloak is the identity provider for the React/API migration.
     */
    private String password;

    private String role;
    private String phone;
    private String skills;

    /**
     * Stable Keycloak identity link. Nullable so existing profiles remain
     * intact until their owners complete the verified account-linking flow.
     */
    @Column(name = "keycloak_subject", unique = true, length = 255)
    private String keycloakSubject;
}