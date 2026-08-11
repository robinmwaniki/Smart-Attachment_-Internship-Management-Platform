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

    private String status = "PENDING";


    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] resumeData;

    private String resumeFilename;

    private String resumeContentType;


    private String feedback;

    public boolean hasResume() {
        return resumeData != null && resumeData.length > 0;
    }
}