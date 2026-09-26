package com.sindibad.App.repository;

import com.sindibad.App.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByGuideGuideId(Long guideId);
    List<Trip> findByCategoryCategoryId(Long categoryId);
    List<Trip> findByTripStatus(String tripStatus);
}