package com.sindibad.App.repository;

import com.sindibad.App.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByPersonPersonId(Long personId);

    boolean existsByPersonPersonId(Long personId);
}