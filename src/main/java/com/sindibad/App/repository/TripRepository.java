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

    // Get all trips with Guide + Person + Category
    // Helps avoid N+1 queries
    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person p
            JOIN FETCH t.category c
            """)
    List<Trip> findAllWithDetails();


    // Get one trip with Guide + Person + Category
    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person p
            JOIN FETCH t.category c
            WHERE t.tripId = :tripId
            """)
    Optional<Trip> findByIdWithDetails(
            @Param("tripId") Long tripId
    );


    // Get trips by status
    List<Trip> findByTripStatus(String tripStatus);


    // Get trips by category
    List<Trip> findByCategoryCategoryId(Long categoryId);


    // Search trips by title
    List<Trip> findByTitleContainingIgnoreCase(String keyword);
}