package com.sindibad.App.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nullTripIdIsRejected() {
        BookingRequestDTO request = new BookingRequestDTO(null, null, 7L, 2);

        assertViolation(validator.validate(request), "tripId", "Trip ID is required");
    }

    @Test
    void presentTripIdPasses() {
        BookingRequestDTO request = new BookingRequestDTO(10L, null, 7L, 2);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void cashPaymentMethodPasses() {
        assertTrue(validator.validate(payment("Cash")).isEmpty());
    }

    @Test
    void paymentMethodOfFiftyCharactersPasses() {
        assertTrue(validator.validate(payment("a".repeat(50))).isEmpty());
    }

    @Test
    void paymentMethodLongerThanFiftyCharactersIsRejected() {
        assertViolation(
                validator.validate(payment("a".repeat(51))),
                "paymentMethod",
                "Payment method cannot exceed 50 characters"
        );
    }

    @Test
    void blankPaymentMethodIsRejected() {
        assertViolation(
                validator.validate(payment(null)),
                "paymentMethod",
                "Payment method is required"
        );
        assertViolation(
                validator.validate(payment("")),
                "paymentMethod",
                "Payment method is required"
        );
        assertViolation(
                validator.validate(payment("   ")),
                "paymentMethod",
                "Payment method is required"
        );
    }

    private PaymentRequestDTO payment(String paymentMethod) {
        return new PaymentRequestDTO(42L, new BigDecimal("1.00"), paymentMethod);
    }

    private <T> void assertViolation(
            Set<ConstraintViolation<T>> violations,
            String property,
            String message) {

        assertEquals(1, violations.size());
        ConstraintViolation<T> violation = violations.iterator().next();
        assertEquals(property, violation.getPropertyPath().toString());
        assertEquals(message, violation.getMessage());
    }
}
