package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Earning;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Payment;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ADMIN_SHARE_PERCENTAGE =
            new BigDecimal("0.15");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EarningRepository earningRepository;
    private final PersonRepository personRepository;
    private final TouristRepository touristRepository;
    private final Clock clock;

    @Autowired
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            EarningRepository earningRepository,
            PersonRepository personRepository,
            TouristRepository touristRepository) {
        this(
                paymentRepository,
                bookingRepository,
                earningRepository,
                personRepository,
                touristRepository,
                Clock.systemDefaultZone()
        );
    }

    PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            EarningRepository earningRepository,
            PersonRepository personRepository,
            TouristRepository touristRepository,
            Clock clock) {

        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.earningRepository = earningRepository;
        this.personRepository = personRepository;
        this.touristRepository = touristRepository;
        this.clock = clock;
    }

    // ==================== Process Payment ====================

    @Override
    @Transactional
    public PaymentResponseDTO processPayment(
            PaymentRequestDTO paymentRequestDTO,
            String actorEmail,
            boolean admin) {

        // 1. Find booking
        Booking booking = bookingRepository
                .findById(paymentRequestDTO.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: "
                                        + paymentRequestDTO.bookingId()
                        )
                );

        if (!admin) {
            assertTouristOwnsBooking(booking, actorEmail);
        }

        // 2. Validate booking status
        if ("Cancelled".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "Cannot process payment for a cancelled booking."
            );
        }

        if ("Confirmed".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "This booking has already been confirmed."
            );
        }

        // 3. Prevent duplicate payment
        if (paymentRepository.existsByBookingBookingId(
                booking.getBookingId())) {

            throw new IllegalArgumentException(
                    "Payment has already been processed for this booking."
            );
        }

        assertTripHasNotStarted(booking.getTrip());

        assertAssignedGuideCanBePaid(booking.getTrip());

        // 4. Calculate the correct amount from the booking
        BigDecimal amount = booking.getTrip()
                .getPricePerPerson()
                .multiply(
                        BigDecimal.valueOf(
                                booking.getNumberOfGuests()
                        )
                )
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Create payment
        Payment payment = new Payment();

        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentMethod(
                paymentRequestDTO.paymentMethod()
        );
        payment.setPaymentStatus("Completed");
        payment.setPaidAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // 6. Confirm booking
        booking.setBookingStatus("Confirmed");
        bookingRepository.save(booking);

        // 7. Calculate revenue shares
        // 15% Admin
        // 85% Guide

        BigDecimal adminShare = amount
                .multiply(ADMIN_SHARE_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal guideShare = amount
                .subtract(adminShare)
                .setScale(2, RoundingMode.HALF_UP);

        // 8. Record earnings
        Earning earning = new Earning();

        earning.setPayment(savedPayment);
        earning.setGuide(booking.getTrip().getGuide());
        earning.setAdminShare(adminShare);
        earning.setGuideShare(guideShare);

        earningRepository.save(earning);

        // 9. Return response
        return toPaymentResponseDTO(savedPayment);
    }

    private void assertAssignedGuideCanBePaid(Trip trip) {
        Guide guide = trip == null ? null : trip.getGuide();
        if (guide == null) {
            throw new IllegalArgumentException(
                    "This trip cannot be paid because it has no assigned guide."
            );
        }

        Person person = guide.getPerson();
        if (person == null) {
            throw new IllegalArgumentException(
                    "This trip cannot be paid because its guide has no account."
            );
        }

        if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
            throw new IllegalArgumentException(
                    "This trip cannot be paid because its guide is not approved."
            );
        }

        if (!"Active".equalsIgnoreCase(person.getAccountStatus())) {
            throw new IllegalArgumentException(
                    "This trip cannot be paid because the guide account is not active."
            );
        }
    }

    private void assertTripHasNotStarted(Trip trip) {
        LocalDateTime start = trip == null ? null : trip.getStartDate();
        if (start == null || !start.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException(
                    "This trip has already started."
            );
        }
    }

    private void assertTouristOwnsBooking(
            Booking booking,
            String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only pay for your own booking."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only pay for your own booking."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only pay for your own booking."
                        )
                );

        Tourist owner = booking.getTourist();

        if (actor.getTouristId() == null
                || owner == null
                || owner.getTouristId() == null
                || !actor.getTouristId().equals(owner.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only pay for your own booking."
            );
        }
    }

    // ==================== Get Payment By ID ====================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(
            Long paymentId,
            String actorEmail,
            boolean admin) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with ID: "
                                        + paymentId
                        )
                );

        if (!admin) {
            assertTouristCanReadPayment(payment, actorEmail);
        }

        return toPaymentResponseDTO(payment);
    }

    private void assertTouristCanReadPayment(
            Payment payment,
            String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own payment."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own payment."
                        )
                );

        Tourist actor = touristRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own payment."
                        )
                );

        Booking booking = payment.getBooking();
        Tourist owner = booking == null ? null : booking.getTourist();

        if (actor.getTouristId() == null
                || owner == null
                || owner.getTouristId() == null
                || !actor.getTouristId().equals(owner.getTouristId())) {

            throw new AccessDeniedException(
                    "You can only view your own payment."
            );
        }
    }

    // ==================== Get Payment By Booking ID ====================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByBookingId(
            Long bookingId,
            String actorEmail,
            boolean admin) {

        Payment payment = paymentRepository
                .findByBookingBookingId(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for Booking ID: "
                                        + bookingId
                        )
                );

        if (!admin) {
            assertTouristCanReadPayment(payment, actorEmail);
        }

        return toPaymentResponseDTO(payment);
    }

    // ==================== Get All Payments ====================

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::toPaymentResponseDTO)
                .toList();
    }

    // ==================== Mapper Helper ====================

    private PaymentResponseDTO toPaymentResponseDTO(
            Payment payment) {

        return new PaymentResponseDTO(
                payment.getPaymentId(),
                payment.getBooking().getBookingId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaidAt()
        );
    }
}