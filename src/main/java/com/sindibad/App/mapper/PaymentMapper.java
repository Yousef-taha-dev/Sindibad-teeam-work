package com.sindibad.App.mapper;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "bookingId", source = "booking.bookingId")
    PaymentResponseDTO toDto(Payment payment);

    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "booking", ignore = true)
    Payment toEntity(PaymentRequestDTO dto);
}