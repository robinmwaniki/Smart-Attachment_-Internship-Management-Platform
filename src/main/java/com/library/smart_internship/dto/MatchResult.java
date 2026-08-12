package com.library.smart_internship.dto;

import com.library.smart_internship.entity.Internship;


public record MatchResult(
        Internship internship,
        int matchPercentage,
        int matchedSkillsCount
) {}