package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;

public interface PaymentService {

    PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO);

    PaymentResponseDTO getPaymentByBookingId(Long bookingId);
}