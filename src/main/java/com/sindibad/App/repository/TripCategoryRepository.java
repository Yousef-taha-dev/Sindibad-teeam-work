package com.sindibad.App.repository;

import com.sindibad.App.entity.TripCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripCategoryRepository extends JpaRepository<TripCategory, Long> {
    Optional<TripCategory> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);

    boolean existsByCategoryNameAndCategoryIdNot(String categoryName, Long categoryId);
}