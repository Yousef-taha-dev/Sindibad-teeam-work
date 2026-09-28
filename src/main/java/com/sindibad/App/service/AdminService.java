package com.sindibad.App.service;

import com.sindibad.App.dto.*;

import java.util.List;

public interface AdminService {

    // ==================== TRIPS ====================

    // إنشاء رحلة جديدة
    TripResponseDTO createTrip(TripRequestDTO tripDTO);

    // تعديل رحلة موجودة
    TripResponseDTO updateTrip(Long tripId, TripRequestDTO tripDTO);

    // جلب رحلة واحدة
    TripResponseDTO getTripById(Long tripId);

    // جلب جميع الرحلات
    List<TripResponseDTO> getAllTrips();

    // حذف رحلة
    void deleteTrip(Long tripId);


    // ==================== GUIDES ====================

    // جلب طلبات المرشدين المعلقة
    List<PendingGuideDTO> getPendingGuides();

    // قبول أو رفض طلب المرشد
    void processGuideApproval(Long guideId, String status);

    // حذف مرشد
    void deleteGuide(Long guideId);

    List<BookingResponseDTO> getGuideBookings();

    // ==================== DASHBOARD ====================

    // إحصائيات لوحة تحكم الأدمن
    AdminDashboardStatsDTO getDashboardStats();
}