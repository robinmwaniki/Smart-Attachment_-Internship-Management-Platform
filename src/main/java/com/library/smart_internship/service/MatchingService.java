package com.library.smart_internship.service;

import com.library.smart_internship.dto.MatchResult;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;

    public List<MatchResult> getRecommendationsForStudent(Long studentId) {
        // 1. Fetch the student safely
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        // 2. Fetch all active internships
        List<Internship> activeInternships = internshipRepository.findByIsActiveTrue();

        // 3. Calculate matches, sort by highest percentage, and return
        return activeInternships.stream()
                .map(internship -> calculateMatch(student, internship))
                .sorted(Comparator.comparingInt(MatchResult::matchPercentage).reversed())
                .collect(Collectors.toList());
    }

    private MatchResult calculateMatch(Student student, Internship internship) {
        // Handle potential null values gracefully to avoid NullPointerExceptions
        String studentSkillsRaw = student.getSkills() != null ? student.getSkills() : "";
        String requiredSkillsRaw = internship.getSkillsRequired() != null ? internship.getSkillsRequired() : "";

        // Convert strings to Sets of formatted words (e.g., "Java, Spring" -> ["java", "spring"])
        Set<String> studentSkills = extractSkills(studentSkillsRaw);
        Set<String> requiredSkills = extractSkills(requiredSkillsRaw);

        if (requiredSkills.isEmpty()) {
            return new MatchResult(internship, 100, 0); // If no skills required, it's a 100% match
        }

        // Count how many required skills the student possesses
        long matchedCount = requiredSkills.stream()
                .filter(studentSkills::contains)
                .count();

        // Calculate the percentage
        int percentage = (int) Math.round((double) matchedCount / requiredSkills.size() * 100);

        return new MatchResult(internship, percentage, (int) matchedCount);
    }

    private Set<String> extractSkills(String skillsString) {
        return Arrays.stream(skillsString.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(skill -> !skill.isEmpty())
                .collect(Collectors.toSet());
    }
}