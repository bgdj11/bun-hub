package com.example.onlybunsbe.infrastructure.messaging;

import com.example.onlybunsbe.DTO.PetCareLocationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    public void sendMessage(String queueName, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            String url = String.format("%s/queue/%s/send", baseUrl, queueName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("📤 Poslata poruka u queue '" + queueName + "': " + json);

        } catch (Exception e) {
            throw new RuntimeException("❌ Neuspešno slanje u queue " + queueName, e);
        }
    }

    public void bindQueueToExchange(String exchange, String queueName) {
        String url = String.format("%s/exchange/%s/bind/%s", baseUrl, exchange, queueName);
        restTemplate.put(url, null);
    }

    public void publishToExchange(String exchange, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String url = String.format("%s/exchange/%s/publish", baseUrl, exchange);
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>(json, h), String.class);
            System.out.println("📣 Fanout publish na exchange '" + exchange + "': " + json);
        } catch (Exception e) {
            throw new RuntimeException("❌ Neuspešno fanout publish na exchange " + exchange, e);
        }
    }
}
