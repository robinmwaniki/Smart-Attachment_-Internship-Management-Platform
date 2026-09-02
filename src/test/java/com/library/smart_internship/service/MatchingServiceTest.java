package com.library.smart_internship.service;

import com.library.smart_internship.dto.MatchResult;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private InternshipRepository internshipRepository;

    @InjectMocks
    private MatchingService matchingService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setSkills("Java, Spring Boot, SQL");
    }

    @Test
    void returnsFullMatchWhenAllRequiredSkillsPresent() {
        Internship internship = new Internship();
        internship.setId(10L);
        internship.setSkillsRequired("java, sql");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(internship));

        List<MatchResult> results = matchingService.getRecommendationsForStudent(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchPercentage()).isEqualTo(100);
        assertThat(results.get(0).matchedSkillsCount()).isEqualTo(2);
    }

    @Test
    void returnsPartialMatchPercentage() {
        Internship internship = new Internship();
        internship.setId(11L);
        internship.setSkillsRequired("java, python, docker");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(internship));

        List<MatchResult> results = matchingService.getRecommendationsForStudent(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedSkillsCount()).isEqualTo(1);
        assertThat(results.get(0).matchPercentage()).isEqualTo(33);
    }

    @Test
    void treatsMissingRequiredSkillsAsFullMatch() {
        Internship internship = new Internship();
        internship.setId(12L);
        internship.setSkillsRequired(null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(internship));

        List<MatchResult> results = matchingService.getRecommendationsForStudent(1L);

        assertThat(results.get(0).matchPercentage()).isEqualTo(100);
        assertThat(results.get(0).matchedSkillsCount()).isEqualTo(0);
    }

    @Test
    void sortsResultsByDescendingMatchPercentage() {
        Internship lowMatch = new Internship();
        lowMatch.setId(1L);
        lowMatch.setSkillsRequired("python, docker, kubernetes");

        Internship highMatch = new Internship();
        highMatch.setId(2L);
        highMatch.setSkillsRequired("java, sql");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(lowMatch, highMatch));

        List<MatchResult> results = matchingService.getRecommendationsForStudent(1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).internship().getId()).isEqualTo(2L);
        assertThat(results.get(1).internship().getId()).isEqualTo(1L);
    }

    @Test
    void throwsWhenStudentNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.getRecommendationsForStudent(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void handlesNullStudentSkillsGracefully() {
        student.setSkills(null);

        Internship internship = new Internship();
        internship.setId(13L);
        internship.setSkillsRequired("java");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(internship));

        List<MatchResult> results = matchingService.getRecommendationsForStudent(1L);

        assertThat(results.get(0).matchedSkillsCount()).isEqualTo(0);
        assertThat(results.get(0).matchPercentage()).isZero();
    }
}