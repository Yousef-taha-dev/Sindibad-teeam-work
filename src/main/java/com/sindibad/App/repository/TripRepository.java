package com.sindibad.App.repository;

import com.sindibad.App.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // ==================== Active / Upcoming Trips ====================

    List<Trip> findByTripStatus(String tripStatus);

    // ==================== Trips By Category ====================

    List<Trip> findByCategoryCategoryId(Long categoryId);

    // ==================== Trips By Guide ====================

    List<Trip> findByGuideGuideId(Long guideId);

    // ==================== Search Trips ====================

    List<Trip> findByTitleContainingIgnoreCase(String keyword);

    // ==================== Get Trip With Details ====================

    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person
            JOIN FETCH t.category c
            WHERE t.tripId = :tripId
            """)
    Optional<Trip> findByIdWithDetails(
            @Param("tripId") Long tripId
    );

    // ==================== Get All Trips With Details ====================

    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person
            JOIN FETCH t.category c
            """)
    List<Trip> findAllWithDetails();
}