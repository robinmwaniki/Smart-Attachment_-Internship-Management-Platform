package com.library.smart_internship.service;

import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternshipService {
    private final InternshipRepository repository;

    public Internship createInternship(Internship internship) {
        return repository.save(internship);
    }


    public List<Internship> getActiveInternships() {
        return repository.findByIsActiveTrue();
    }
}