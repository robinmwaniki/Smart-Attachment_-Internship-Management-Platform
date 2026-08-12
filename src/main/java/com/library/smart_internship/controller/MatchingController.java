package com.library.smart_internship.controller;

import com.library.smart_internship.dto.MatchResult;
import com.library.smart_internship.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;


    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<MatchResult>> getRecommendations(@PathVariable Long studentId) {
        List<MatchResult> recommendations = matchingService.getRecommendationsForStudent(studentId);
        return ResponseEntity.ok(recommendations);
    }
}
