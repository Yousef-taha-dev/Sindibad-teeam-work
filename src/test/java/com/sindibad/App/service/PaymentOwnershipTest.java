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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOwnershipTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EarningRepository earningRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TouristRepository touristRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                bookingRepository,
                earningRepository,
                personRepository,
                touristRepository
        );
    }

    @Test
    void touristCanPayOwnPendingBooking() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        PaymentRequestDTO request = request(new BigDecimal("1.00"));

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(100L);
            return payment;
        });

        PaymentResponseDTO response = paymentService.processPayment(
                request,
                "lina@example.com",
                false
        );

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(100L, response.paymentId());
        assertEquals(42L, response.bookingId());
        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals("Cash", response.paymentMethod());
        assertEquals("Completed", response.paymentStatus());
        assertEquals(new BigDecimal("20.00"), savedPayment.getAmount());
        assertEquals("Completed", savedPayment.getPaymentStatus());
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(bookingRepository).save(booking);

        ArgumentCaptor<Earning> earningCaptor = ArgumentCaptor.forClass(Earning.class);
        verify(earningRepository).save(earningCaptor.capture());
        Earning earning = earningCaptor.getValue();
        assertEquals(new BigDecimal("3.00"), earning.getAdminShare());
        assertEquals(new BigDecimal("17.00"), earning.getGuideShare());
        assertEquals(9L, earning.getGuide().getGuideId());
    }

    @Test
    void touristCannotPayAnotherTouristsBooking() {
        Booking booking = pendingBooking(tourist(8L, 4L));

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void adminCanPayAnotherTouristsBooking() {
        Booking booking = pendingBooking(tourist(8L, 4L));

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(100L);
            return payment;
        });

        PaymentResponseDTO response = paymentService.processPayment(
                request(new BigDecimal("1.00")),
                "admin@example.com",
                true
        );

        assertEquals(100L, response.paymentId());
        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals("Completed", response.paymentStatus());
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(booking);
        verify(earningRepository).save(any(Earning.class));
    }

    @Test
    void missingBookingIsRejected() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.processPayment(
                        new PaymentRequestDTO(99L, new BigDecimal("1.00"), "Cash"),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Booking not found with ID: 99", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void cancelledBookingIsRejected() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        booking.setBookingStatus("Cancelled");

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "Cannot process payment for a cancelled booking.",
                exception.getMessage()
        );
        assertEquals("Cancelled", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void confirmedBookingIsRejected() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        booking.setBookingStatus("Confirmed");

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "This booking has already been confirmed.",
                exception.getMessage()
        );
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
        verify(paymentRepository, never()).existsByBookingBookingId(any());
    }

    @Test
    void secondPaymentForTheSameBookingIsRejected() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "Payment has already been processed for this booking.",
                exception.getMessage()
        );
        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void missingEmailIsRejected() {
        Booking booking = pendingBooking(tourist(7L, 3L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        null,
                        false
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "  ",
                        false
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(personRepository, never()).findByEmail(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void missingPersonIsRejected() {
        Booking booking = pendingBooking(tourist(7L, 3L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(touristRepository, never()).findByPersonPersonId(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void missingTouristIsRejected() {
        Booking booking = pendingBooking(tourist(7L, 3L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void bookingWithoutTouristIsRejected() {
        Booking booking = pendingBooking(tourist(7L, 3L));
        booking.setTourist(null);

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    @Test
    void suspendedGuideCannotBePaid() {
        rejectUnpayableGuide(
                "Suspended",
                "Active",
                "This trip cannot be paid because its guide is not approved."
        );
    }

    @Test
    void rejectedGuideCannotBePaid() {
        rejectUnpayableGuide(
                "Rejected",
                "Active",
                "This trip cannot be paid because its guide is not approved."
        );
    }

    @Test
    void inactiveGuideAccountCannotBePaid() {
        rejectUnpayableGuide(
                "Approved",
                "Inactive",
                "This trip cannot be paid because the guide account is not active."
        );
    }

    @Test
    void missingGuideCannotBePaid() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        booking.getTrip().setGuide(null);
        stubOwner(booking, tourist);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "This trip cannot be paid because it has no assigned guide.",
                exception.getMessage()
        );
        assertPaymentWasNotSaved(booking);
    }

    @Test
    void guideWithoutAccountCannotBePaid() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        booking.getTrip().getGuide().setPerson(null);
        stubOwner(booking, tourist);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "This trip cannot be paid because its guide has no account.",
                exception.getMessage()
        );
        assertPaymentWasNotSaved(booking);
    }

    @Test
    void adminCannotPayBookingWithSuspendedGuide() {
        Booking booking = pendingBooking(tourist(8L, 4L));
        booking.getTrip().getGuide().setApprovalStatus("Suspended");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "admin@example.com",
                        true
                )
        );

        assertEquals(
                "This trip cannot be paid because its guide is not approved.",
                exception.getMessage()
        );
        assertEquals("Pending", booking.getBookingStatus());
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    private void rejectUnpayableGuide(
            String approvalStatus,
            String accountStatus,
            String expectedMessage) {

        Tourist tourist = tourist(7L, 3L);
        Booking booking = pendingBooking(tourist);
        Guide guide = booking.getTrip().getGuide();
        guide.setApprovalStatus(approvalStatus);
        guide.getPerson().setAccountStatus(accountStatus);
        stubOwner(booking, tourist);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        request(new BigDecimal("1.00")),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(expectedMessage, exception.getMessage());
        assertPaymentWasNotSaved(booking);
    }

    private void stubOwner(Booking booking, Tourist tourist) {
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
    }

    private void assertPaymentWasNotSaved(Booking booking) {
        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(earningRepository, never()).save(any(Earning.class));
    }

    private PaymentRequestDTO request(BigDecimal amount) {
        return new PaymentRequestDTO(42L, amount, "Cash");
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

    private Booking pendingBooking(Tourist tourist) {
        Person guideAccount = new Person();
        guideAccount.setPersonId(11L);
        guideAccount.setAccountStatus("Active");

        Guide guide = new Guide();
        guide.setGuideId(9L);
        guide.setPerson(guideAccount);
        guide.setApprovalStatus("Approved");

        Trip trip = new Trip();
        trip.setPricePerPerson(new BigDecimal("10.00"));
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setGuide(guide);

        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist);
        booking.setTrip(trip);
        booking.setNumberOfGuests(2);
        booking.setBookingStatus("Pending");
        return booking;
    }
}
