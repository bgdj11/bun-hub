package com.example.onlybunsbe.repository;

import com.example.onlybunsbe.model.PetCareLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetCareLocationRepository extends JpaRepository<PetCareLocation, Long> {
    Optional<PetCareLocation> findByExternalId(Long externalId);
}
