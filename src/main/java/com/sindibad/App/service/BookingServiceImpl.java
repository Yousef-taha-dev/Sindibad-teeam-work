package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TouristRepository touristRepository;
    private final PersonRepository personRepository;
    private final BookingMapper bookingMapper;
    private final PaymentRepository paymentRepository;
    private final GuideRepository guideRepository;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public BookingServiceImpl(BookingRepository bookingRepository,
                              TripRepository tripRepository,
                              TouristRepository touristRepository,
                              PersonRepository personRepository,
                              BookingMapper bookingMapper,
                              PaymentRepository paymentRepository,
                              GuideRepository guideRepository) {
        this(
                bookingRepository,
                tripRepository,
                touristRepository,
                personRepository,
                bookingMapper,
                paymentRepository,
                guideRepository,
                null,
                Clock.systemDefaultZone()
        );
    }

    BookingServiceImpl(BookingRepository bookingRepository,
                       TripRepository tripRepository,
                       TouristRepository touristRepository,
                       PersonRepository personRepository,
                       BookingMapper bookingMapper,
                       PaymentRepository paymentRepository,
                       GuideRepository guideRepository,
                       EntityManager entityManager) {
        this(
                bookingRepository,
                tripRepository,
                touristRepository,
                personRepository,
                bookingMapper,
                paymentRepository,
                guideRepository,
                entityManager,
                Clock.systemDefaultZone()
        );
    }

    BookingServiceImpl(BookingRepository bookingRepository,
                       TripRepository tripRepository,
                       TouristRepository touristRepository,
                       PersonRepository personRepository,
                       BookingMapper bookingMapper,
                       PaymentRepository paymentRepository,
                       GuideRepository guideRepository,
                       EntityManager entityManager,
                       Clock clock) {

        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.touristRepository = touristRepository;
        this.personRepository = personRepository;
        this.bookingMapper = bookingMapper;
        this.paymentRepository = paymentRepository;
        this.guideRepository = guideRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(
            Long touristId,
            BookingRequestDTO bookingDTO,
            String actorEmail,
            boolean admin) {

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

        assertTripHasNotStarted(trip);

        // Find tourist
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tourist not found with ID: " + touristId
                        )
                );

        if (!admin) {
            assertTouristCanCreateBooking(tourist, actorEmail);
        }

        entityManager.refresh(trip, LockModeType.PESSIMISTIC_WRITE);

        if (!"Upcoming".equals(trip.getTripStatus())) {
            throw new IllegalArgumentException(
                    "This trip is not available for booking."
            );
        }

        assertTripHasNotStarted(trip);

        assertAssignedGuideCanTakeBookings(trip);

        // Calculate currently booked guests
        long currentBookedGuests =
                bookingRepository.sumGuestsByTripId(trip.getTripId());
        long requestedGuests = bookingDTO.numberOfGuests();
        long maximumParticipants = trip.getMaxParticipants();

        // Check available capacity
        if (currentBookedGuests + requestedGuests > maximumParticipants) {

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
    public BookingResponseDTO getBookingById(
            Long bookingId,
            String actorEmail,
            boolean admin,
            boolean guide) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: " + bookingId
                        )
                );

        if (!admin) {
            if (guide) {
                assertGuideCanReadBooking(booking, actorEmail);
            } else {
                assertTouristCanReadBooking(booking, actorEmail);
            }
        }

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getTouristBookings(
            Long touristId,
            String actorEmail,
            boolean admin) {

        Tourist requestedTourist = touristRepository.findById(touristId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tourist not found with ID: " + touristId
                        )
                );

        if (!admin) {
            assertTouristCanListBookings(requestedTourist, actorEmail);
        }

        return bookingRepository
                .findByTouristTouristId(touristId)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    private void assertTouristCanListBookings(
            Tourist requestedTourist,
            String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own bookings."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own bookings."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own bookings."
                        )
                );

        if (actor.getTouristId() == null
                || requestedTourist.getTouristId() == null
                || !actor.getTouristId().equals(requestedTourist.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only view your own bookings."
            );
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, String actorEmail, boolean admin) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: " + bookingId
                        )
                );

        if (!admin) {
            assertTouristOwnsBooking(booking, actorEmail);
        }

        // Prevent cancelling an already cancelled booking
        if ("Cancelled".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "Booking is already cancelled."
            );
        }

        if ("Confirmed".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "This booking has already been confirmed."
            );
        }

        if (paymentRepository.existsByBookingBookingId(booking.getBookingId())) {
            throw new IllegalArgumentException(
                    "Cannot cancel a booking that has already been paid."
            );
        }

        booking.setBookingStatus("Cancelled");

        bookingRepository.save(booking);
    }

    private void assertGuideCanReadBooking(Booking booking, String actorEmail) {
        Guide actor = authenticatedGuide(actorEmail);
        Guide assigned = assignedGuide(booking);

        if (actor.getGuideId() == null
                || assigned == null
                || assigned.getGuideId() == null
                || !actor.getGuideId().equals(assigned.getGuideId())) {
            throw new AccessDeniedException(
                    "You can only view bookings assigned to you."
            );
        }
    }

    private Guide authenticatedGuide(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only view bookings assigned to you."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view bookings assigned to you."
                        )
                );

        if (person.getPersonId() == null
                || !"Active".equalsIgnoreCase(person.getAccountStatus())) {
            throw new AccessDeniedException(
                    "You can only view bookings assigned to you."
            );
        }

        Guide guide = guideRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view bookings assigned to you."
                        )
                );

        if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
            throw new AccessDeniedException(
                    "You can only view bookings assigned to you."
            );
        }

        return guide;
    }

    private Guide assignedGuide(Booking booking) {
        Trip trip = booking.getTrip();
        if (trip == null) {
            return null;
        }
        return trip.getGuide();
    }

    private void assertTouristCanReadBooking(Booking booking, String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own booking."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own booking."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own booking."
                        )
                );

        Tourist owner = booking.getTourist();

        if (owner == null
                || owner.getTouristId() == null
                || !owner.getTouristId().equals(actor.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only view your own booking."
            );
        }
    }

    private void assertTripHasNotStarted(Trip trip) {
        LocalDateTime start = trip.getStartDate();
        if (start == null || !start.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException(
                    "This trip has already started."
            );
        }
    }

    private void assertAssignedGuideCanTakeBookings(Trip trip) {
        Guide guide = trip.getGuide();
        if (guide == null) {
            throw new IllegalArgumentException(
                    "This trip cannot be booked because it has no assigned guide."
            );
        }

        Person person = guide.getPerson();
        if (person == null) {
            throw new IllegalArgumentException(
                    "This trip cannot be booked because its guide has no account."
            );
        }

        if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
            throw new IllegalArgumentException(
                    "This trip cannot be booked because its guide is not approved."
            );
        }

        if (!"Active".equalsIgnoreCase(person.getAccountStatus())) {
            throw new IllegalArgumentException(
                    "This trip cannot be booked because the guide account is not active."
            );
        }
    }

    private void assertTouristCanCreateBooking(
            Tourist requestedTourist,
            String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only create your own booking."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only create your own booking."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only create your own booking."
                        )
                );

        if (actor.getTouristId() == null
                || requestedTourist.getTouristId() == null
                || !actor.getTouristId().equals(requestedTourist.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only create your own booking."
            );
        }
    }

    private void assertTouristOwnsBooking(Booking booking, String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only cancel your own booking."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only cancel your own booking."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only cancel your own booking."
                        )
                );

        Tourist owner = booking.getTourist();

        if (owner == null
                || owner.getTouristId() == null
                || !owner.getTouristId().equals(actor.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only cancel your own booking."
            );
        }
    }

    {
    }
}
