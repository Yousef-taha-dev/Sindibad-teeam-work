package com.sindibad.App.repository;

import com.sindibad.App.entity.Earning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EarningRepository extends JpaRepository<Earning, Long> {
    List<Earning> findByGuideGuideId(Long guideId);
}