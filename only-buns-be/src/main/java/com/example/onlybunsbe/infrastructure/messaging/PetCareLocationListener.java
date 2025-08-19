package com.example.onlybunsbe.infrastructure.messaging;

import com.example.onlybunsbe.DTO.PetCareLocationDTO;
import com.example.onlybunsbe.model.PetCareLocation;
import com.example.onlybunsbe.service.PetCareLocationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PetCareLocationListener {

    private final PetCareLocationService service;

    public PetCareLocationListener(PetCareLocationService service) {
        this.service = service;
    }

    @RabbitListener(queues = "rabbit_locations")
    public void receive(PetCareLocationDTO dto) {
        PetCareLocation loc = new PetCareLocation();
        loc.setExternalId(dto.getId());   // id iz druge aplikacije
        loc.setName(dto.getName());
        loc.setCountry(dto.getCountry());
        loc.setCity(dto.getCity());
        loc.setAddress(dto.getAddress());
        loc.setNumber(dto.getNumber());
        loc.setLatitude(dto.getLatitude());
        loc.setLongitude(dto.getLongitude());

        service.save(loc);
        System.out.println("✅ Primljena care lokacija: " + dto.getName());
    }
}
