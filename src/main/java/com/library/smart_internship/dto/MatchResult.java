package com.library.smart_internship.dto;

import com.library.smart_internship.entity.Internship;

// A Record is a modern, concise way to create immutable DTOs in Java
public record MatchResult(
        Internship internship,
        int matchPercentage,
        int matchedSkillsCount
) {}