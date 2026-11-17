package com.library.smart_internship.repository;

import com.library.smart_internship.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByEmailIgnoreCase(String email);

    Optional<Student> findByKeycloakSubject(String keycloakSubject);
}