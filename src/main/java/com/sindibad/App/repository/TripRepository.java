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

    // جلب جميع الرحلات مع Guide + Person + Category
    // لتجنب N+1 Query
    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person p
            JOIN FETCH t.category c
            """)
    List<Trip> findAllWithDetails();


    // جلب رحلة واحدة مع Guide + Person + Category
    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person p
            JOIN FETCH t.category c
            WHERE t.tripId = :tripId
            """)
    Optional<Trip> findByIdWithDetails(@Param("tripId") Long tripId);
}