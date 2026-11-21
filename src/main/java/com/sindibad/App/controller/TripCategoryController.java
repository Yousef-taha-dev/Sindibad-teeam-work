package com.sindibad.App.controller;

import com.sindibad.App.dto.TripCategoryDTO;
import com.sindibad.App.service.TripCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(
        name = "Trip categories",
        description = "Public endpoints for browsing trip categories"
)
public class TripCategoryController {

    private final TripCategoryService tripCategoryService;

    public TripCategoryController(TripCategoryService tripCategoryService) {
        this.tripCategoryService = tripCategoryService;
    }

    @Operation(
            summary = "Get all categories",
            description = "Returns every trip category. No authentication is required."
    )
    @GetMapping
    public ResponseEntity<List<TripCategoryDTO>> getAllCategories() {

        return ResponseEntity.ok(tripCategoryService.getAllCategories());
    }
}
