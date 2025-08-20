package com.example.onlybunsbe.infrastructure.messaging;

import com.example.onlybunsbe.DTO.PetCareLocationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomQueueClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl = "http://localhost:8085"; // Queue service app

    public PetCareLocationDTO receiveLocation() {
        String url = baseUrl + "/queue/rabbit_locations/receive";
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);

        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            try {
                return objectMapper.readValue(res.getBody(), PetCareLocationDTO.class);
            } catch (Exception e) {
                throw new RuntimeException("❌ Neuspešna deserializacija: " + res.getBody(), e);
            }
        }
        return null;
    }
}
