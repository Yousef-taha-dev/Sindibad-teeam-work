package com.sindibad.App.service;

import com.sindibad.App.dto.TripCategoryDTO;
import com.sindibad.App.entity.TripCategory;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.TripCategoryMapper;
import com.sindibad.App.repository.TripCategoryRepository;
import com.sindibad.App.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripCategoryServiceTest {

    @Mock
    private TripCategoryRepository tripCategoryRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripCategoryMapper tripCategoryMapper;

    private TripCategoryServiceImpl tripCategoryService;

    @BeforeEach
    void setUp() {
        tripCategoryService = new TripCategoryServiceImpl(
                tripCategoryRepository,
                tripRepository,
                tripCategoryMapper
        );
    }

    @Test
    void createCategorySavesTrimmedNameAndDescription() {
        when(tripCategoryRepository.existsByCategoryName("History")).thenReturn(false);
        when(tripCategoryRepository.save(any(TripCategory.class))).thenAnswer(invocation -> {
            TripCategory category = invocation.getArgument(0);
            assertNull(category.getCategoryId());
            category.setCategoryId(5L);
            return category;
        });
        when(tripCategoryMapper.toDto(any(TripCategory.class))).thenAnswer(this::dtoFromCategory);

        TripCategoryDTO result = tripCategoryService.createCategory(
                new TripCategoryDTO(null, "  History  ", "  Guided walks  ")
        );

        ArgumentCaptor<TripCategory> saved = ArgumentCaptor.forClass(TripCategory.class);
        verify(tripCategoryRepository).save(saved.capture());
        assertEquals("History", saved.getValue().getCategoryName());
        assertEquals("Guided walks", saved.getValue().getDescription());
        assertEquals(5L, result.categoryId());
        assertEquals("History", result.categoryName());
        assertEquals("Guided walks", result.description());
    }

    @Test
    void blankCategoryNameIsRejected() {
        assertRejectedName(null);
        assertRejectedName("");
        assertRejectedName("   ");
        verify(tripCategoryRepository, never()).save(any());
    }

    @Test
    void duplicateCategoryNameIsRejected() {
        when(tripCategoryRepository.existsByCategoryName("History")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tripCategoryService.createCategory(
                        new TripCategoryDTO(null, "History", "Guided walks")
                )
        );

        assertEquals("Category name is already in use.", exception.getMessage());
        verify(tripCategoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryKeepsItsCurrentName() {
        TripCategory category = category(5L, "History", "Old description");
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(tripCategoryRepository.existsByCategoryNameAndCategoryIdNot("History", 5L))
                .thenReturn(false);
        when(tripCategoryRepository.save(category)).thenReturn(category);
        when(tripCategoryMapper.toDto(category)).thenAnswer(this::dtoFromCategory);

        TripCategoryDTO result = tripCategoryService.updateCategory(
                5L,
                new TripCategoryDTO(5L, " History ", "  Updated description  ")
        );

        assertEquals(5L, category.getCategoryId());
        assertEquals("History", category.getCategoryName());
        assertEquals("Updated description", category.getDescription());
        assertEquals("History", result.categoryName());
        assertEquals("Updated description", result.description());
        verify(tripCategoryRepository).save(category);
    }

    @Test
    void updateMissingCategoryIsRejected() {
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tripCategoryService.updateCategory(
                        5L,
                        new TripCategoryDTO(5L, "History", "Guided walks")
                )
        );

        assertEquals("Category not found with ID: 5", exception.getMessage());
        verify(tripCategoryRepository, never()).save(any());
        verify(tripCategoryRepository, never())
                .existsByCategoryNameAndCategoryIdNot(any(), any());
    }

    @Test
    void updateRejectsNameUsedByAnotherCategory() {
        TripCategory category = category(5L, "History", "Guided walks");
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(tripCategoryRepository.existsByCategoryNameAndCategoryIdNot("Desert", 5L))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tripCategoryService.updateCategory(
                        5L,
                        new TripCategoryDTO(5L, "Desert", "Dunes")
                )
        );

        assertEquals("Category name is already in use.", exception.getMessage());
        assertEquals("History", category.getCategoryName());
        assertEquals("Guided walks", category.getDescription());
        verify(tripCategoryRepository, never()).save(any());
    }

    @Test
    void deleteCategoryWhenNoTripReferencesIt() {
        TripCategory category = category(5L, "History", "Guided walks");
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(tripRepository.existsByCategoryCategoryId(5L)).thenReturn(false);

        tripCategoryService.deleteCategory(5L);

        verify(tripCategoryRepository).delete(category);
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void deleteCategoryReferencedByTripIsRejected() {
        TripCategory category = category(5L, "History", "Guided walks");
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(tripRepository.existsByCategoryCategoryId(5L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tripCategoryService.deleteCategory(5L)
        );

        assertEquals(
                "This category cannot be deleted because it is used by trips.",
                exception.getMessage()
        );
        verify(tripCategoryRepository, never()).delete(any());
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void deleteMissingCategoryIsRejected() {
        when(tripCategoryRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tripCategoryService.deleteCategory(5L)
        );

        assertEquals("Category not found with ID: 5", exception.getMessage());
        verify(tripRepository, never()).existsByCategoryCategoryId(any());
        verify(tripCategoryRepository, never()).delete(any());
    }

    @Test
    void publicListingReturnsCategoryDtos() {
        TripCategory history = category(5L, "History", "Guided walks");
        TripCategory desert = category(6L, "Desert", null);
        when(tripCategoryRepository.findAll()).thenReturn(List.of(history, desert));
        when(tripCategoryMapper.toDto(history)).thenAnswer(this::dtoFromCategory);
        when(tripCategoryMapper.toDto(desert)).thenAnswer(this::dtoFromCategory);

        List<TripCategoryDTO> categories = tripCategoryService.getAllCategories();

        assertEquals(2, categories.size());
        assertEquals(5L, categories.get(0).categoryId());
        assertEquals("History", categories.get(0).categoryName());
        assertEquals("Guided walks", categories.get(0).description());
        assertEquals(6L, categories.get(1).categoryId());
        assertEquals("Desert", categories.get(1).categoryName());
        assertNull(categories.get(1).description());
        verify(tripCategoryRepository, never()).save(any());
        verify(tripCategoryRepository, never()).delete(any());
    }

    private void assertRejectedName(String categoryName) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tripCategoryService.createCategory(
                        new TripCategoryDTO(null, categoryName, "Guided walks")
                )
        );

        assertEquals("Category name is required.", exception.getMessage());
    }

    private TripCategoryDTO dtoFromCategory(org.mockito.invocation.InvocationOnMock invocation) {
        TripCategory category = invocation.getArgument(0);
        return new TripCategoryDTO(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription()
        );
    }

    private TripCategory category(Long categoryId, String name, String description) {
        TripCategory category = new TripCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(name);
        category.setDescription(description);
        return category;
    }
}
