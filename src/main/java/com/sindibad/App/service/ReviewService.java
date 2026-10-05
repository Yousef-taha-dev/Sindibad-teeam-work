package com.sindibad.App.service;

import com.sindibad.App.dto.ReviewRequestDTO;
import com.sindibad.App.dto.ReviewResponseDTO;

import java.util.List;

public interface ReviewService {

    ReviewResponseDTO createReview(
            Long touristId,
            ReviewRequestDTO reviewDTO
    );

    List<ReviewResponseDTO> getReviewsByTripId(Long tripId);
}