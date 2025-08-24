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

    @Value("${queue.exchange:ads}")
    private String exchange;

    @Value("${queue.name:ads-agency-web-2}")
    private String queueName;

    /** Pozovi na startup: samoregistracija/bind na exchange. Idempotentno. */
    public void bindQueueToExchange() {
        String url = String.format("%s/exchange/%s/bind/%s", baseUrl, exchange, queueName);
        restTemplate.put(url, null);
        System.out.println("🔗 Bound queue '" + queueName + "' to exchange '" + exchange + "'");
    }

    /** Poll sledeće poruke iz TVOG queue-a (non-blocking). */
    public String receiveMessage() {
        String url = String.format("%s/queue/%s/receive", baseUrl, queueName);
        ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
        // 204 No Content => nema poruka
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            return res.getBody();
        }
        return null;
    }
}
