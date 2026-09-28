package com.sindibad.App.repository;

import com.sindibad.App.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTouristTouristId(Long touristId);
    List<Booking> findByTripTripId(Long tripId);
    List<Booking> findByBookingStatus(String bookingStatus);
    @Query("SELECT b FROM Booking b WHERE b.guide IS NOT NULL")
    List<Booking> findAllGuideBookings();
}