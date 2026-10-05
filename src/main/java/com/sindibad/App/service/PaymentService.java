package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentResponseDTO getPaymentByBookingId(Long bookingId);

    // الميثودز الجديدة
    PaymentResponseDTO getPaymentById(Long paymentId);

    List<PaymentResponseDTO> getAllPayments();
}