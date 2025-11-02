package com.rabbitcare.bunny_org_app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitcare.bunny_org_app.config.RabbitConfig;
import com.rabbitcare.bunny_org_app.customqueue.CustomQueueClient;
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
    private final CustomQueueClient queueClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocationSenderService(RabbitTemplate rabbitTemplate,
                                 LocationRepository repo,
                                 CustomQueueClient queueClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.repo = repo;
        this.queueClient = queueClient;
    }

    public void sendAllLocations() {
        List<Location> all = repo.findAll();
        for (Location loc : all) {
            LocationDTO dto = new LocationDTO();
            dto.setId(loc.getId());
            dto.setName(loc.getName());
            dto.setCountry(loc.getCountry());
            dto.setCity(loc.getCity());
            dto.setAddress(loc.getAddress());
            dto.setNumber(loc.getNumber());
            dto.setLatitude(loc.getLatitude());
            dto.setLongitude(loc.getLongitude());

            // 1) Stari RabbitMQ mehanizam
            // rabbitTemplate.convertAndSend(RabbitConfig.QUEUE, dto);

            // 2) Novi CustomQueue servis (JSON)
            try {
                String json = objectMapper.writeValueAsString(dto);
                queueClient.createQueue("rabbit_locations");
                queueClient.sendJson(RabbitConfig.QUEUE, json);
                System.out.println("📤 Poslata lokacija (custom-queue): " + dto.getName()
                        + " (" + dto.getLatitude() + ", " + dto.getLongitude() + ")");
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize LocationDTO", e);
            }
        }
    }
}

