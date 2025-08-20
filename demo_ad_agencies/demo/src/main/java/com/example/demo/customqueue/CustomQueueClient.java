package com.example.demo.customqueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CustomQueueClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${queue.service.base-url:http://localhost:8085}")
    private String baseUrl;

    private static final String QUEUE_NAME = "ad_posts";

    public String receiveMessage() {
        String url = String.format("%s/queue/%s/receive", baseUrl, QUEUE_NAME);
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
        if (res.getStatusCode().is2xxSuccessful()) {
            return res.getBody();
        }
        return null;
    }
}
