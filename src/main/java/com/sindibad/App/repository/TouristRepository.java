package com.sindibad.App.repository;

import com.sindibad.App.entity.Tourist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Long> {
    Optional<Tourist> findByPersonPersonId(Long personId);
    boolean existsByPersonPersonId(Long personId);
}