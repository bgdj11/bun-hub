package com.rabbitcare.bunny_org_app.customqueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomQueueClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${queue.service.base-url:http://localhost:8085}")
    private String baseUrl;


    public void createQueue(String queueName) {
        String url = String.format("%s/queue/%s", baseUrl, queueName);
        restTemplate.put(url, null);
    }


    public void sendJson(String queueName, String jsonPayload) {
        String url = String.format("%s/queue/%s/send", baseUrl, queueName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    public String receiveJson(String queueName) {
        String url = String.format("%s/queue/%s/receive", baseUrl, queueName);
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
        if (res.getStatusCode().is2xxSuccessful()) {
            return res.getBody();
        }
        return null;
    }
}
