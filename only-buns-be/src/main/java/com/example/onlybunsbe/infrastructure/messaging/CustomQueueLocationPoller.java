package com.example.onlybunsbe.infrastructure.messaging;

import com.example.onlybunsbe.DTO.PetCareLocationDTO;
import com.example.onlybunsbe.model.PetCareLocation;
import com.example.onlybunsbe.service.PetCareLocationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CustomQueueLocationPoller {

    private final CustomQueueClient queueClient;
    private final PetCareLocationService service;

    public CustomQueueLocationPoller(CustomQueueClient queueClient, PetCareLocationService service) {
        this.queueClient = queueClient;
        this.service = service;
    }

    // svakih 5 sekundi proverava queue
    @Scheduled(fixedDelay = 5000)
    public void pollQueue() {
        try {
            PetCareLocationDTO dto = queueClient.receiveLocation();
            if (dto != null) {
                PetCareLocation loc = new PetCareLocation();
                loc.setExternalId(dto.getId());
                loc.setName(dto.getName());
                loc.setCountry(dto.getCountry());
                loc.setCity(dto.getCity());
                loc.setAddress(dto.getAddress());
                loc.setNumber(dto.getNumber());
                loc.setLatitude(dto.getLatitude());
                loc.setLongitude(dto.getLongitude());

                service.save(loc);
                System.out.println("✅ Primljena care lokacija (custom-queue): " + dto.getName());
            }
        }catch (Exception e){
        }

        }
    }

