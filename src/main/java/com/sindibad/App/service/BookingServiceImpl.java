package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TouristRepository touristRepository;
    private final BookingMapper bookingMapper;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              TripRepository tripRepository,
                              TouristRepository touristRepository,
                              BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.touristRepository = touristRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(
            Long touristId,
            BookingRequestDTO bookingDTO) {

        // Validate number of guests
        if (bookingDTO.numberOfGuests() <= 0) {
            throw new IllegalArgumentException(
                    "Number of guests must be greater than zero."
            );
        }

        // Find trip
        Trip trip = tripRepository.findById(bookingDTO.tripId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found with ID: "
                                        + bookingDTO.tripId()
                        )
                );

        // Check trip status
        if (!"Upcoming".equals(trip.getTripStatus())) {
            throw new IllegalArgumentException(
                    "This trip is not available for booking."
            );
        }

        // Find tourist
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tourist not found with ID: " + touristId
                        )
                );

        // Calculate currently booked guests
        int currentBookedGuests =
                bookingRepository.sumGuestsByTripId(trip.getTripId());

        // Check available capacity
        if (currentBookedGuests
                + bookingDTO.numberOfGuests()
                > trip.getMaxParticipants()) {

            throw new IllegalArgumentException(
                    "Requested seats exceed maximum available capacity for this trip."
            );
        }

        // Create booking
        Booking booking = new Booking();

        booking.setTrip(trip);
        booking.setTourist(tourist);
        booking.setBookingDate(LocalDateTime.now());
        booking.setNumberOfGuests(bookingDTO.numberOfGuests());
        booking.setBookingStatus("Pending");

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        // Convert to DTO
        return bookingMapper.toDto(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: " + bookingId
                        )
                );

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getTouristBookings(
            Long touristId) {

        // Verify tourist exists
        touristRepository.findById(touristId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tourist not found with ID: " + touristId
                        )
                );

        return bookingRepository
                .findByTouristTouristId(touristId)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: " + bookingId
                        )
                );

        // Prevent cancelling an already cancelled booking
        if ("Cancelled".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "Booking is already cancelled."
            );
        }

        booking.setBookingStatus("Cancelled");

        bookingRepository.save(booking);
    }

    {
    }
}
