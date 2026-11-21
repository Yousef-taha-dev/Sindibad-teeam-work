package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    PaymentResponseDTO processPayment(
            PaymentRequestDTO paymentRequestDTO,
            String actorEmail,
            boolean admin);

    PaymentResponseDTO getPaymentByBookingId(
            Long bookingId,
            String actorEmail,
            boolean admin);

    // الميثودز الجديدة
    PaymentResponseDTO getPaymentById(
            Long paymentId,
            String actorEmail,
            boolean admin);

    List<PaymentResponseDTO> getAllPayments();
}