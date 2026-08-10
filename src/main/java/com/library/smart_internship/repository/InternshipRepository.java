package com.library.smart_internship.repository;

import com.library.smart_internship.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByRecruiterId(Long recruiterId);

    List<Internship> findByTitleContainingIgnoreCaseOrSkillsRequiredContainingIgnoreCase(String title, String skills);


    List<Internship> findByIsActiveTrue();
}