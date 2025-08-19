package com.example.onlybunsbe.controller;

import com.example.onlybunsbe.model.PetCareLocation;
import com.example.onlybunsbe.service.PetCareLocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/care-locations")
public class PetCareLocationController {

    private final PetCareLocationService service;

    public PetCareLocationController(PetCareLocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PetCareLocation> getAll() {
        return service.findAll();
    }
}
