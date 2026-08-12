package com.library.smart_internship.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String skillsRequired;
    private boolean isActive = true;

    @Column(name = "slots_available")
    private Integer slotsAvailable;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private Student recruiter;
}

