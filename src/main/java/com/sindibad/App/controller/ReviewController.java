package com.sindibad.App.controller;

import com.sindibad.App.dto.ReviewRequestDTO;
import com.sindibad.App.dto.ReviewResponseDTO;
import com.sindibad.App.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(
        name = "Reviews",
        description = "Endpoints for creating, retrieving, and managing trip reviews"
)
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // ==================== Create Review ====================

    @Operation(
            summary = "Create a review",
            description = "Allows a tourist to review a confirmed booking."
    )
    @PostMapping("/tourist/{touristId}")
    @PreAuthorize("hasRole('TOURIST')")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @PathVariable Long touristId,
            @Valid @RequestBody ReviewRequestDTO reviewDTO) {

        ReviewResponseDTO response =
                reviewService.createReview(
                        touristId,
                        reviewDTO
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==================== Get Reviews By Trip ====================

    @Operation(
            summary = "Get trip reviews",
            description = "Returns all reviews submitted for a specific trip."
    )
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByTripId(
            @PathVariable Long tripId) {

        return ResponseEntity.ok(
                reviewService.getReviewsByTripId(tripId)
        );
    }

    // ==================== Delete Review ====================

    @Operation(
            summary = "Delete a review",
            description = "Allows an Admin or the review owner to delete a review by its ID."
    )
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId) {

        reviewService.deleteReview(reviewId);

        return ResponseEntity.noContent().build();
    }
}