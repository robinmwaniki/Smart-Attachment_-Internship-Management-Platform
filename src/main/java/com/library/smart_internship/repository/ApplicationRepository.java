package com.library.smart_internship.repository;

import com.library.smart_internship.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {


    List<Application> findByInternshipRecruiterId(Long recruiterId);


    List<Application> findByInternshipId(Long internshipId);


    List<Application> findByStudentId(Long studentId);
}