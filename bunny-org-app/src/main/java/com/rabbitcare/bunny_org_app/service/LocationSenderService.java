package com.rabbitcare.bunny_org_app.service;

import com.rabbitcare.bunny_org_app.config.RabbitConfig;
import com.rabbitcare.bunny_org_app.dto.LocationDTO;
import com.rabbitcare.bunny_org_app.model.Location;
import com.rabbitcare.bunny_org_app.repository.LocationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationSenderService {

    private final RabbitTemplate rabbitTemplate;
    private final LocationRepository repo;

    public LocationSenderService(RabbitTemplate rabbitTemplate, LocationRepository repo) {
        this.rabbitTemplate = rabbitTemplate;
        this.repo = repo;
    }

    public void sendAllLocations() {
        List<Location> locations = repo.findAll();
        for (Location loc : locations) {
            LocationDTO dto = new LocationDTO();
            dto.setId(loc.getId());
            dto.setName(loc.getName());
            dto.setCountry(loc.getCountry());
            dto.setCity(loc.getCity());
            dto.setAddress(loc.getAddress());
            dto.setNumber(loc.getNumber());
            dto.setLatitude(loc.getLatitude());
            dto.setLongitude(loc.getLongitude());

            rabbitTemplate.convertAndSend(RabbitConfig.QUEUE, dto);
            System.out.println("📤 Poslata lokacija: " + dto.getName() + " (" + dto.getLatitude() + ", " + dto.getLongitude() + ")");
        }
    }
}
