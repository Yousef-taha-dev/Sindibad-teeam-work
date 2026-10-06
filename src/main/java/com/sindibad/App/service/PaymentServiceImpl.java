package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Earning;
import com.sindibad.App.entity.Payment;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ADMIN_SHARE_PERCENTAGE =
            new BigDecimal("0.15");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EarningRepository earningRepository;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            EarningRepository earningRepository) {

        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.earningRepository = earningRepository;
    }

    // ==================== Process Payment ====================

    @Override
    @Transactional
    public PaymentResponseDTO processPayment(
            PaymentRequestDTO paymentRequestDTO) {

        // 1. Find booking
        Booking booking = bookingRepository
                .findById(paymentRequestDTO.bookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: "
                                        + paymentRequestDTO.bookingId()
                        )
                );

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

    // ==================== Get Payment By ID ====================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with ID: "
                                        + paymentId
                        )
                );

        return toPaymentResponseDTO(payment);
    }

    // ==================== Get Payment By Booking ID ====================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByBookingId(Long bookingId) {

        Payment payment = paymentRepository
                .findByBookingBookingId(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for Booking ID: "
                                        + bookingId
                        )
                );

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