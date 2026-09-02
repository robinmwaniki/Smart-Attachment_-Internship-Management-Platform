package com.library.smart_internship.service;

import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.repository.InternshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternshipServiceTest {

    @Mock
    private InternshipRepository internshipRepository;

    @InjectMocks
    private InternshipService internshipService;

    @Test
    void createInternshipDelegatesToRepository() {
        Internship internship = new Internship();
        internship.setTitle("Backend Intern");

        when(internshipRepository.save(internship)).thenReturn(internship);

        Internship result = internshipService.createInternship(internship);

        assertThat(result).isEqualTo(internship);
    }

    @Test
    void getActiveInternshipsReturnsOnlyActiveOnes() {
        Internship active = new Internship();
        active.setId(1L);
        active.setActive(true);

        when(internshipRepository.findByIsActiveTrue()).thenReturn(List.of(active));

        List<Internship> results = internshipService.getActiveInternships();

        assertThat(results).containsExactly(active);
    }
}