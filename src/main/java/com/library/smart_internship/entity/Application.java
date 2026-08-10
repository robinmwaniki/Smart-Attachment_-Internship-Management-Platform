package com.library.smart_internship.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id")
    private Internship internship;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    // NEW: Stores the file path of the uploaded resume attachment
    private String resumePath;

    // Add this field inside Application.java
    private String feedback; // Stores interview details or feedback message
}