package com.sindibad.App.service;

import com.sindibad.App.dto.ReviewRequestDTO;
import com.sindibad.App.dto.ReviewResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Review;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.ReviewMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             BookingRepository bookingRepository,
                             ReviewMapper reviewMapper) {

        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(
            Long touristId,
            ReviewRequestDTO reviewDTO) {

        // 1. Find booking
        Booking booking = bookingRepository
                .findById(reviewDTO.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: "
                                        + reviewDTO.bookingId()
                        )
                );

        // 2. Validate booking ownership
        if (!booking.getTourist()
                .getTouristId()
                .equals(touristId)) {

            throw new IllegalArgumentException(
                    "You can only review trips that you have booked."
            );
        }

        // 3. Validate booking status
        if (!"Confirmed".equals(booking.getBookingStatus())) {

            throw new IllegalArgumentException(
                    "Cannot review a booking that is not confirmed."
            );
        }

        // 4. Prevent duplicate reviews
        if (reviewRepository
                .findByBookingBookingId(booking.getBookingId())
                .isPresent()) {

            throw new IllegalArgumentException(
                    "A review has already been submitted for this booking."
            );
        }

        // 5. Create review
        Review review = new Review();

        review.setBooking(booking);
        review.setRating(reviewDTO.rating());
        review.setComment(reviewDTO.comment());


        // 6. Save review
        Review savedReview = reviewRepository.save(review);

        // 7. Convert Entity to DTO
        return reviewMapper.toDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByTripId(Long tripId) {

        return reviewRepository
                .findByBookingTripTripId(tripId)
                .stream()
                .map(reviewMapper::toDto)
                .toList();
    }
}