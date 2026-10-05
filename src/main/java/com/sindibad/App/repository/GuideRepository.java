package com.sindibad.App.repository;

import com.sindibad.App.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {
    List<Guide> findByApprovalStatus(String approvalStatus);

    boolean existsByPersonPersonId(Long personId);
    long countByApprovalStatus(String approvalStatus);
}