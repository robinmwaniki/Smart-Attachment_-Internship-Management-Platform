package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {


    private final InternshipService internshipService;


    @PostMapping
    public ResponseEntity<Internship> createInternship(@Valid @RequestBody Internship internship) {
        Internship savedInternship = internshipService.createInternship(internship);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedInternship);
    }


    @GetMapping("/active")
    public ResponseEntity<List<Internship>> getActiveInternships() {
        List<Internship> activeInternships = internshipService.getActiveInternships();

        return ResponseEntity.ok(activeInternships);
    }
}