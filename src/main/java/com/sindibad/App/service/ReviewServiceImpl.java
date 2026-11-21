package com.sindibad.App.service;

import com.sindibad.App.dto.ReviewRequestDTO;
import com.sindibad.App.dto.ReviewResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Review;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.ReviewMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.ReviewRepository;
import com.sindibad.App.repository.TouristRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final PersonRepository personRepository;
    private final TouristRepository touristRepository;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            PersonRepository personRepository,
            TouristRepository touristRepository,
            ReviewMapper reviewMapper) {

        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.personRepository = personRepository;
        this.touristRepository = touristRepository;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(
            Long touristId,
            ReviewRequestDTO reviewDTO,
            String actorEmail) {

        assertTouristOwnsPath(touristId, actorEmail);

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
                .map(this::withoutBookingId)
                .toList();
    }

    private ReviewResponseDTO withoutBookingId(ReviewResponseDTO response) {
        return new ReviewResponseDTO(
                response.reviewId(),
                null,
                response.touristName(),
                response.tripTitle(),
                response.rating(),
                response.comment()
        );
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, String actorEmail, boolean admin) {

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review not found with ID: " + reviewId
                        )
                );

        if (!admin) {
            assertTouristOwnsReview(review, actorEmail);
        }

        reviewRepository.delete(review);
    }

    private void assertTouristOwnsPath(Long touristId, String actorEmail) {
        Tourist actor = authenticatedTourist(
                actorEmail,
                "You can only create your own review."
        );

        if (actor.getTouristId() == null
                || touristId == null
                || !actor.getTouristId().equals(touristId)) {
            throw new AccessDeniedException(
                    "You can only create your own review."
            );
        }
    }

    private void assertTouristOwnsReview(Review review, String actorEmail) {
        Tourist actor = authenticatedTourist(
                actorEmail,
                "You can only delete your own review."
        );

        Booking booking = review.getBooking();
        Tourist owner = booking == null ? null : booking.getTourist();

        if (actor.getTouristId() == null
                || owner == null
                || owner.getTouristId() == null
                || !actor.getTouristId().equals(owner.getTouristId())) {
            throw new AccessDeniedException(
                    "You can only delete your own review."
            );
        }
    }

    private Tourist authenticatedTourist(String actorEmail, String message) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(message);
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new AccessDeniedException(message));

        return touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() -> new AccessDeniedException(message));
    }
}