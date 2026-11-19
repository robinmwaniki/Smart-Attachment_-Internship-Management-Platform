package com.library.smart_internship.service;

import com.library.smart_internship.entity.Application;
import com.library.smart_internship.entity.Internship;
import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.ApplicationRepository;
import com.library.smart_internship.repository.InternshipRepository;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-concurrency;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@org.springframework.transaction.annotation.Transactional(
        propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class ApplicationConcurrencyIT {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    @Autowired
    private StudentRepository studentRepository;

    private final TransactionTemplate transactions;

    private Long studentId;
    private Long internshipId;
    private Long applicationId;

    @Autowired
    ApplicationConcurrencyIT(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @BeforeEach
    void createIsolatedFixture() {
        transactions.executeWithoutResult(status -> {
            applicationRepository.deleteAll();
            internshipRepository.deleteAll();
            studentRepository.deleteAll();

            Student student = new Student();
            student.setName("Concurrency Test Student");
            student.setEmail("concurrency@example.test");
            student.setRole("STUDENT");
            studentId = studentRepository.saveAndFlush(student).getId();

            Internship internship = new Internship();
            internship.setTitle("Concurrency Test Internship");
            internship.setCompany("Test Company");
            internshipId = internshipRepository.saveAndFlush(internship).getId();

            Application application = new Application();
            application.setStudent(studentRepository.findById(studentId).orElseThrow());
            application.setInternship(internshipRepository.findById(internshipId).orElseThrow());
            application.setStatus("PENDING");
            applicationId = applicationRepository.saveAndFlush(application).getId();
        });
    }

    @Test
    void databaseConstraintRejectsDuplicateStudentInternshipApplications() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            Application duplicate = new Application();
            duplicate.setStudent(studentRepository.findById(studentId).orElseThrow());
            duplicate.setInternship(internshipRepository.findById(internshipId).orElseThrow());
            duplicate.setStatus("PENDING");
            applicationRepository.saveAndFlush(duplicate);
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optimisticLockRejectsStaleConcurrentApplicationUpdate() {
        Application firstRead = transactions.execute(status ->
                applicationRepository.findById(applicationId).orElseThrow());
        Application staleRead = transactions.execute(status ->
                applicationRepository.findById(applicationId).orElseThrow());

        transactions.executeWithoutResult(status -> {
            firstRead.setStatus("APPROVED");
            applicationRepository.saveAndFlush(firstRead);
        });

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            staleRead.setStatus("REJECTED");
            applicationRepository.saveAndFlush(staleRead);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void optimisticLockRejectsStaleConcurrentInternshipUpdate() {
        Internship firstRead = transactions.execute(status ->
                internshipRepository.findById(internshipId).orElseThrow());
        Internship staleRead = transactions.execute(status ->
                internshipRepository.findById(internshipId).orElseThrow());

        transactions.executeWithoutResult(status -> {
            firstRead.setTitle("Updated Internship Title");
            internshipRepository.saveAndFlush(firstRead);
        });

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            staleRead.setTitle("Conflicting Internship Title");
            internshipRepository.saveAndFlush(staleRead);
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
