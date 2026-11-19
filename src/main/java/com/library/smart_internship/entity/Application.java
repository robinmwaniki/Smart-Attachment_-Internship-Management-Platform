package com.library.smart_internship.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_application_student_internship",
        columnNames = {"student_id", "internship_id"}
))
@Getter
@Setter
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Detects stale concurrent edits instead of silently overwriting them. */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id")
    private Internship internship;

    private String status = "PENDING";

    @Column(columnDefinition = "bytea")
    private byte[] resumeData;

    private String resumeFilename;

    private String resumeContentType;

    private String feedback;

    private String applicantName;

    private Integer applicantAge;

    private String applicantPhone;

    @Column(columnDefinition = "text")
    private String coverLetter;

    private LocalDateTime interviewDateTime;

    public boolean hasResume() {
        return resumeData != null && resumeData.length > 0;
    }
}