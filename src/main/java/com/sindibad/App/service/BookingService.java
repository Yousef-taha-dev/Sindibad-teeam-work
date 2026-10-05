package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;

import java.util.List;

public interface BookingService {

    /**
     * إنشاء حجز جديد لسائح محدد بعد التحقق من السعة المتاحة للرحلة
     */
    BookingResponseDTO createBooking(Long touristId, BookingRequestDTO bookingDTO);

    /**
     * جلب تفاصيل حجز معين بواسطة معرف الحجز
     */
    BookingResponseDTO getBookingById(Long bookingId);

    /**
     * جلب قائمة جميع الحجوزات الخاصة بسائح محدد
     */
    List<BookingResponseDTO> getTouristBookings(Long touristId);

    /**
     * إلغاء حجز قائم وتحديث حالته إلى Cancelled
     */
    void cancelBooking(Long bookingId);
}