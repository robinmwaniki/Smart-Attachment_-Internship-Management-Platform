package com.library.smart_internship.service;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private InternshipRepository internshipRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void appliesWithResumeAndSavesApplication() throws Exception {
        Student student = new Student();
        student.setId(1L);

        Internship internship = new Internship();
        internship.setId(2L);

        MultipartFile resume = new MockMultipartFile(
                "resume", "cv.pdf", "application/pdf", "content".getBytes());

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findById(2L)).thenReturn(Optional.of(internship));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyWithResume(1L, 2L, resume);

        assertThat(result.getStudent()).isEqualTo(student);
        assertThat(result.getInternship()).isEqualTo(internship);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.hasResume()).isTrue();
        assertThat(result.getResumeFilename()).isEqualTo("cv.pdf");
        assertThat(result.getResumeContentType()).isEqualTo("application/pdf");
    }

    @Test
    void appliesWithoutResumeWhenFileIsEmpty() throws Exception {
        Student student = new Student();
        student.setId(1L);

        Internship internship = new Internship();
        internship.setId(2L);

        MultipartFile emptyFile = new MockMultipartFile("resume", new byte[0]);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findById(2L)).thenReturn(Optional.of(internship));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyWithResume(1L, 2L, emptyFile);

        assertThat(result.hasResume()).isFalse();
    }

    @Test
    void throwsWhenStudentNotFoundOnApply() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.applyWithResume(1L, 2L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void throwsWhenInternshipNotFoundOnApply() {
        Student student = new Student();
        student.setId(1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(internshipRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.applyWithResume(1L, 2L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Internship not found");
    }

    @Test
    void getApplicationsForInternshipDelegatesToRepository() {
        Application application = new Application();
        when(applicationRepository.findByInternshipId(5L)).thenReturn(List.of(application));

        List<Application> results = applicationService.getApplicationsForInternship(5L);

        assertThat(results).containsExactly(application);
    }

    @Test
    void updateApplicationStatusUppercasesStatus() {
        Application application = new Application();
        application.setId(3L);
        application.setStatus("PENDING");

        when(applicationRepository.findById(3L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.updateApplicationStatus(3L, "accepted");

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void updateApplicationStatusWithFeedbackSetsBothFields() {
        Application application = new Application();
        application.setId(4L);

        when(applicationRepository.findById(4L)).thenReturn(Optional.of(application));
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        when(applicationRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        applicationService.updateApplicationStatus(4L, "rejected", "Not enough experience");

        Application saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("REJECTED");
        assertThat(saved.getFeedback()).isEqualTo("Not enough experience");
    }

    @Test
    void throwsWhenApplicationNotFoundOnStatusUpdate() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplicationStatus(99L, "accepted"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Application not found");

        verify(applicationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void authenticatedStudentCanApplyWithoutSupplyingStudentId() throws Exception {
        Student student = new Student();
        student.setId(11L);
        student.setName("Test Student");
        student.setPhone("0700000000");
        student.setRole("STUDENT");
        student.setKeycloakSubject("student-sub");

        Internship internship = new Internship();
        internship.setId(22L);
        internship.setTitle("Backend Intern");

        when(studentRepository.findByKeycloakSubject("student-sub")).thenReturn(Optional.of(student));
        when(internshipRepository.findById(22L)).thenReturn(Optional.of(internship));
        when(applicationRepository.findByStudentId(11L)).thenReturn(List.of());
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> {
                    Application saved = invocation.getArgument(0);
                    saved.setId(33L);
                    return saved;
                });

        var response = applicationService.submitForAuthenticatedStudent("student-sub", 22L, null);

        assertThat(response.id()).isEqualTo(33L);
        assertThat(response.applicantName()).isEqualTo("Test Student");
        assertThat(response.internshipId()).isEqualTo(22L);
    }

    @Test
    void recruiterCanOnlyListApplicationsForOwnedInternship() {
        Student recruiter = new Student();
        recruiter.setId(7L);
        recruiter.setRole("RECRUITER");
        recruiter.setKeycloakSubject("company-sub");

        Student applicant = new Student();
        applicant.setId(8L);
        applicant.setName("Applicant");
        applicant.setEmail("applicant@example.com");

        Internship internship = new Internship();
        internship.setId(12L);
        internship.setTitle("Java Intern");
        internship.setRecruiter(recruiter);

        Application application = new Application();
        application.setId(13L);
        application.setStudent(applicant);
        application.setInternship(internship);
        application.setStatus("PENDING");

        when(studentRepository.findByKeycloakSubject("company-sub")).thenReturn(Optional.of(recruiter));
        when(internshipRepository.findById(12L)).thenReturn(Optional.of(internship));
        when(applicationRepository.findByInternshipId(12L)).thenReturn(List.of(application));

        var responses = applicationService.getApplicationsForRecruiter("company-sub", 12L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().applicantEmail()).isEqualTo("applicant@example.com");
    }

    @Test
    void recruiterCannotListApplicationsForAnotherCompany() {
        Student caller = new Student();
        caller.setId(7L);
        caller.setRole("RECRUITER");
        caller.setKeycloakSubject("company-a");

        Student owner = new Student();
        owner.setId(9L);
        owner.setRole("RECRUITER");

        Internship internship = new Internship();
        internship.setId(12L);
        internship.setRecruiter(owner);

        when(studentRepository.findByKeycloakSubject("company-a")).thenReturn(Optional.of(caller));
        when(internshipRepository.findById(12L)).thenReturn(Optional.of(internship));

        assertThatThrownBy(() -> applicationService.getApplicationsForRecruiter("company-a", 12L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(applicationRepository, org.mockito.Mockito.never()).findByInternshipId(12L);
    }

    @Test
    void studentCannotDownloadAnotherStudentsResume() {
        Student caller = new Student();
        caller.setId(7L);
        caller.setRole("STUDENT");
        caller.setKeycloakSubject("student-a");

        Student owner = new Student();
        owner.setId(8L);

        Application application = new Application();
        application.setId(4L);
        application.setStudent(owner);
        application.setResumeData("private cv".getBytes());
        application.setInternship(new Internship());

        when(studentRepository.findByKeycloakSubject("student-a")).thenReturn(Optional.of(caller));
        when(applicationRepository.findById(4L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.downloadResumeForPrincipal("student-a", 4L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void studentCanDownloadOwnResume() {
        Student student = new Student();
        student.setId(7L);
        student.setRole("STUDENT");
        student.setKeycloakSubject("student-a");

        Application application = new Application();
        application.setId(4L);
        application.setStudent(student);
        application.setResumeData("my cv".getBytes());
        application.setResumeFilename("cv.pdf");
        application.setResumeContentType("application/pdf");

        when(studentRepository.findByKeycloakSubject("student-a")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(4L)).thenReturn(Optional.of(application));

        var resume = applicationService.downloadResumeForPrincipal("student-a", 4L);

        assertThat(new String(resume.data())).isEqualTo("my cv");
        assertThat(resume.filename()).isEqualTo("cv.pdf");
    }

}
