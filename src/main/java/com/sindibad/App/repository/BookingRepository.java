package com.sindibad.App.repository;

import com.sindibad.App.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTouristTouristId(Long touristId);

    @Query("""
            SELECT COALESCE(SUM(b.numberOfGuests), 0)
            FROM Booking b
            WHERE b.trip.tripId = :tripId
              AND b.bookingStatus != 'Cancelled'
            """)
    Integer sumGuestsByTripId(@Param("tripId") Long tripId);

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.trip t
            JOIN FETCH t.guide g
            JOIN FETCH g.person p
            JOIN FETCH b.tourist tourist
            JOIN FETCH tourist.person touristPerson
            """)
    List<Booking> findAllGuideBookings();
}