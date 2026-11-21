package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;

import java.util.List;

public interface BookingService {

    /**
     * إنشاء حجز جديد لسائح محدد بعد التحقق من السعة المتاحة للرحلة
     */
    BookingResponseDTO createBooking(
            Long touristId,
            BookingRequestDTO bookingDTO,
            String actorEmail,
            boolean admin
    );

    /**
     * جلب تفاصيل حجز معين بواسطة معرف الحجز.
     * A tourist may read only a booking they own. A guide may read a booking
     * only when that guide is assigned to the booking's trip. An admin may read any booking.
     */
    BookingResponseDTO getBookingById(
            Long bookingId,
            String actorEmail,
            boolean admin,
            boolean guide
    );

    /**
     * جلب قائمة جميع الحجوزات الخاصة بسائح محدد.
     * A tourist may list only their own bookings. An admin may list any tourist's bookings.
     */
    List<BookingResponseDTO> getTouristBookings(
            Long touristId,
            String actorEmail,
            boolean admin
    );

    /**
     * إلغاء حجز قائم وتحديث حالته إلى Cancelled.
     * A tourist may cancel only a booking they own. An admin may cancel any booking.
     */
    void cancelBooking(Long bookingId, String actorEmail, boolean admin);
}