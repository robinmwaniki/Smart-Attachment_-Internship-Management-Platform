package com.library.smart_internship.service;

import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudentDelegatesToRepository() {
        Student student = new Student();
        student.setEmail("jane@example.com");

        when(studentRepository.save(student)).thenReturn(student);

        Student result = studentService.createStudent(student);

        assertThat(result).isEqualTo(student);
    }

    @Test
    void getAllStudentsReturnsRepositoryResult() {
        Student first = new Student();
        first.setId(1L);
        Student second = new Student();
        second.setId(2L);

        when(studentRepository.findAll()).thenReturn(List.of(first, second));

        List<Student> results = studentService.getAllStudents();

        assertThat(results).containsExactly(first, second);
    }
}