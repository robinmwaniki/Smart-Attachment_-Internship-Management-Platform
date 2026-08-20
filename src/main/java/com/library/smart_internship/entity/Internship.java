package com.library.smart_internship.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String companyWebsite;
    private String category;
    private String skillsRequired;
    private boolean isActive = true;

    @Column(name = "slots_available")
    private Integer slotsAvailable;

    private LocalDate applicationDeadline;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private Student recruiter;

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDate.now());
    }
}