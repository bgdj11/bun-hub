package com.example.onlybunsbe.service;

import com.example.onlybunsbe.model.PetCareLocation;
import com.example.onlybunsbe.repository.PetCareLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetCareLocationService {

    private final PetCareLocationRepository repository;

    public PetCareLocationService(PetCareLocationRepository repository) {
        this.repository = repository;
    }

    public PetCareLocation save(PetCareLocation location) {
        return repository.save(location);
    }

    public List<PetCareLocation> findAll() {
        return repository.findAll();
    }
}
