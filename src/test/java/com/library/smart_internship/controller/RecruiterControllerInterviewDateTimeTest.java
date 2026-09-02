package com.library.smart_internship.controller;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import com.library.smart_internship.service.EmailService;
import com.library.smart_internship.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecruiterControllerInterviewDateTimeTest {

    @Mock
    private InternshipRepository internshipRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    private MockMvc mockMvc;

    private Application application;

    @BeforeEach
    void setUp() {
        RecruiterController controller = new RecruiterController(
                internshipRepository, studentRepository, applicationRepository, emailService, smsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Student student = new Student();
        student.setName("Jane Doe");
        student.setEmail("jane@example.com");
        student.setPhone("0712345678");

        Internship internship = new Internship();
        internship.setTitle("Backend Intern");

        application = new Application();
        application.setId(1L);
        application.setStatus("PENDING");
        application.setStudent(student);
        application.setInternship(internship);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        lenient().when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(internshipRepository.save(any(Internship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reviewApplicationSetsInterviewDateTimeWhenProvided() throws Exception {
        mockMvc.perform(post("/recruiter/applications/1/review")
                        .param("status", "APPROVED")
                        .param("feedback", "Great fit")
                        .param("interviewDateTime", "2026-09-15T14:30"))
                .andExpect(status().is3xxRedirection());

        assertThat(application.getInterviewDateTime()).isEqualTo(LocalDateTime.of(2026, 9, 15, 14, 30));
    }

    @Test
    void reviewApplicationLeavesInterviewDateTimeNullWhenNotProvided() throws Exception {
        mockMvc.perform(post("/recruiter/applications/1/review")
                        .param("status", "APPROVED")
                        .param("feedback", "Great fit"))
                .andExpect(status().is3xxRedirection());

        assertThat(application.getInterviewDateTime()).isNull();
    }

    @Test
    void reviewApplicationIgnoresMalformedInterviewDateTime() throws Exception {
        mockMvc.perform(post("/recruiter/applications/1/review")
                        .param("status", "APPROVED")
                        .param("feedback", "Great fit")
                        .param("interviewDateTime", "not-a-date"))
                .andExpect(status().is3xxRedirection());

        assertThat(application.getInterviewDateTime()).isNull();
        assertThat(application.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void bulkReviewDoesNotOverwriteExistingInterviewDateTime() throws Exception {
        LocalDateTime existing = LocalDateTime.of(2026, 9, 10, 9, 0);
        application.setInterviewDateTime(existing);

        mockMvc.perform(post("/recruiter/applications/bulk-review")
                        .param("applicationIds", "1")
                        .param("status", "APPROVED"))
                .andExpect(status().is3xxRedirection());

        assertThat(application.getInterviewDateTime()).isEqualTo(existing);
    }
}