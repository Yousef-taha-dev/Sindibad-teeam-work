package com.sindibad.App.service;

import com.sindibad.App.dto.ReviewRequestDTO;
import com.sindibad.App.dto.ReviewResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Review;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.mapper.ReviewMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.ReviewRepository;
import com.sindibad.App.repository.TouristRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewOwnershipTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private ReviewMapper reviewMapper;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository,
                bookingRepository,
                personRepository,
                touristRepository,
                reviewMapper
        );
    }

    @Test
    void touristCanReviewOwnConfirmedBooking() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = confirmedBooking(tourist);
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingBookingId(42L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(15L);
            return review;
        });
        when(reviewMapper.toDto(any(Review.class))).thenReturn(
                new ReviewResponseDTO(15L, 42L, "Lina Haddad", "Petra", 5, "Great")
        );

        ReviewResponseDTO response = reviewService.createReview(
                7L,
                new ReviewRequestDTO(42L, 5, "Great"),
                "lina@example.com"
        );

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertSame(booking, saved.getValue().getBooking());
        assertEquals(5, saved.getValue().getRating());
        assertEquals("Great", saved.getValue().getComment());
        assertEquals(15L, response.reviewId());
    }

    @Test
    void pendingBookingCannotBeReviewed() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = confirmedBooking(tourist);
        booking.setBookingStatus("Pending");
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(
                        7L,
                        new ReviewRequestDTO(42L, 5, "Great"),
                        "lina@example.com"
                )
        );

        assertEquals(
                "Cannot review a booking that is not confirmed.",
                exception.getMessage()
        );
        assertEquals("Pending", booking.getBookingStatus());
        verify(reviewRepository, never()).findByBookingBookingId(42L);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void duplicateReviewForTheSameBookingIsRejected() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = confirmedBooking(tourist);
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(reviewOwnedBy(tourist)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(
                        7L,
                        new ReviewRequestDTO(42L, 5, "Great"),
                        "lina@example.com"
                )
        );

        assertEquals(
                "A review has already been submitted for this booking.",
                exception.getMessage()
        );
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void touristCannotCreateReviewForAnotherTouristId() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> reviewService.createReview(
                        8L,
                        new ReviewRequestDTO(42L, 5, "Great"),
                        "lina@example.com"
                )
        );

        assertEquals("You can only create your own review.", exception.getMessage());
        verify(bookingRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void touristCannotDeleteAnotherTouristsReview() {
        Review review = reviewOwnedBy(tourist(8L, 4L));
        when(reviewRepository.findById(15L)).thenReturn(Optional.of(review));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> reviewService.deleteReview(15L, "lina@example.com", false)
        );

        assertEquals("You can only delete your own review.", exception.getMessage());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void guideCannotDeleteAReview() {
        Review review = reviewOwnedBy(tourist(8L, 4L));
        when(reviewRepository.findById(15L)).thenReturn(Optional.of(review));
        when(personRepository.findByEmail("guide@example.com"))
                .thenReturn(Optional.of(person(9L)));
        when(touristRepository.findByPersonPersonId(9L)).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> reviewService.deleteReview(15L, "guide@example.com", false)
        );

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void ownerCanDeleteOwnReview() {
        Tourist tourist = tourist(7L, 3L);
        Review review = reviewOwnedBy(tourist);
        when(reviewRepository.findById(15L)).thenReturn(Optional.of(review));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));

        reviewService.deleteReview(15L, "lina@example.com", false);

        verify(reviewRepository).delete(review);
    }

    @Test
    void adminCanDeleteAnyReview() {
        Review review = reviewOwnedBy(tourist(8L, 4L));
        when(reviewRepository.findById(15L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(15L, "admin@example.com", true);

        verify(reviewRepository).delete(review);
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    private Person person(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("lina@example.com");
        return person;
    }

    private Tourist tourist(Long touristId, Long personId) {
        Tourist tourist = new Tourist();
        tourist.setTouristId(touristId);
        tourist.setPerson(person(personId));
        return tourist;
    }

    private Booking confirmedBooking(Tourist tourist) {
        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist);
        booking.setBookingStatus("Confirmed");
        return booking;
    }

    private Review reviewOwnedBy(Tourist tourist) {
        Review review = new Review();
        review.setReviewId(15L);
        review.setBooking(confirmedBooking(tourist));
        return review;
    }
}
