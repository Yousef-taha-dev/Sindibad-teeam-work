package com.sindibad.App.service;

import com.sindibad.App.dto.TripCategoryDTO;
import com.sindibad.App.entity.TripCategory;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.TripCategoryMapper;
import com.sindibad.App.repository.TripCategoryRepository;
import com.sindibad.App.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripCategoryServiceImpl implements TripCategoryService {

    private static final int MAX_CATEGORY_NAME_LENGTH = 100;

    private final TripCategoryRepository tripCategoryRepository;
    private final TripRepository tripRepository;
    private final TripCategoryMapper tripCategoryMapper;

    public TripCategoryServiceImpl(
            TripCategoryRepository tripCategoryRepository,
            TripRepository tripRepository,
            TripCategoryMapper tripCategoryMapper) {

        this.tripCategoryRepository = tripCategoryRepository;
        this.tripRepository = tripRepository;
        this.tripCategoryMapper = tripCategoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripCategoryDTO> getAllCategories() {

        return tripCategoryRepository.findAll()
                .stream()
                .map(tripCategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TripCategoryDTO createCategory(TripCategoryDTO categoryDTO) {

        String categoryName = requiredCategoryName(categoryDTO);

        if (tripCategoryRepository.existsByCategoryName(categoryName)) {
            throw new IllegalArgumentException(
                    "Category name is already in use."
            );
        }

        TripCategory category = new TripCategory();
        category.setCategoryName(categoryName);
        category.setDescription(normalizedDescription(categoryDTO));

        return tripCategoryMapper.toDto(tripCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public TripCategoryDTO updateCategory(Long categoryId, TripCategoryDTO categoryDTO) {

        TripCategory category = tripCategoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId
                        )
                );

        String categoryName = requiredCategoryName(categoryDTO);

        if (tripCategoryRepository.existsByCategoryNameAndCategoryIdNot(
                categoryName,
                categoryId)) {
            throw new IllegalArgumentException(
                    "Category name is already in use."
            );
        }

        category.setCategoryName(categoryName);
        category.setDescription(normalizedDescription(categoryDTO));

        return tripCategoryMapper.toDto(tripCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {

        TripCategory category = tripCategoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with ID: " + categoryId
                        )
                );

        if (tripRepository.existsByCategoryCategoryId(categoryId)) {
            throw new IllegalArgumentException(
                    "This category cannot be deleted because it is used by trips."
            );
        }

        tripCategoryRepository.delete(category);
    }

    private String requiredCategoryName(TripCategoryDTO categoryDTO) {

        if (categoryDTO == null
                || categoryDTO.categoryName() == null
                || categoryDTO.categoryName().isBlank()) {
            throw new IllegalArgumentException(
                    "Category name is required."
            );
        }

        String categoryName = categoryDTO.categoryName().trim();

        if (categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Category name cannot exceed 100 characters."
            );
        }

        return categoryName;
    }

    private String normalizedDescription(TripCategoryDTO categoryDTO) {

        if (categoryDTO.description() == null) {
            return null;
        }

        String description = categoryDTO.description().trim();
        return description.isEmpty() ? null : description;
    }
}
