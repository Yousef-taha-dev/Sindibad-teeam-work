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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewListBookingIdTest {

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
    void tripReviewListOmitsBookingIdAndKeepsTheOtherFields() {
        Review review = new Review();
        review.setReviewId(15L);
        ReviewResponseDTO mapped = new ReviewResponseDTO(
                15L,
                42L,
                "Lina Haddad",
                "Petra by night",
                5,
                "Great"
        );
        when(reviewRepository.findByBookingTripTripId(10L)).thenReturn(List.of(review));
        when(reviewMapper.toDto(review)).thenReturn(mapped);

        List<ReviewResponseDTO> responses = reviewService.getReviewsByTripId(10L);

        assertEquals(1, responses.size());
        ReviewResponseDTO response = responses.get(0);
        assertNull(response.bookingId());
        assertEquals(15L, response.reviewId());
        assertEquals("Lina Haddad", response.touristName());
        assertEquals("Petra by night", response.tripTitle());
        assertEquals(5, response.rating());
        assertEquals("Great", response.comment());
    }

    @Test
    void createReviewStillReturnsBookingId() {
        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);
        Person person = new Person();
        person.setPersonId(3L);
        tourist.setPerson(person);

        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist);
        booking.setBookingStatus("Confirmed");

        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingBookingId(42L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewMapper.toDto(any(Review.class))).thenReturn(new ReviewResponseDTO(
                15L,
                42L,
                "Lina Haddad",
                null,
                5,
                "Great"
        ));

        ReviewResponseDTO response = reviewService.createReview(
                7L,
                new ReviewRequestDTO(42L, 5, "Great"),
                "lina@example.com"
        );

        assertEquals(42L, response.bookingId());
        assertEquals(15L, response.reviewId());
        assertEquals("Lina Haddad", response.touristName());
        assertEquals(5, response.rating());
        assertEquals("Great", response.comment());
    }
}
