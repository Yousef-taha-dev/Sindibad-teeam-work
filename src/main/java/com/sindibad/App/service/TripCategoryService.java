package com.sindibad.App.service;

import com.sindibad.App.dto.TripCategoryDTO;

import java.util.List;

public interface TripCategoryService {

    List<TripCategoryDTO> getAllCategories();

    TripCategoryDTO createCategory(TripCategoryDTO categoryDTO);

    TripCategoryDTO updateCategory(Long categoryId, TripCategoryDTO categoryDTO);

    void deleteCategory(Long categoryId);
}
