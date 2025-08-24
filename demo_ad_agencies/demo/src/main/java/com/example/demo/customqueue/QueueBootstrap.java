package com.example.demo.customqueue;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class QueueBootstrap {

    private final CustomQueueClient client;

    public QueueBootstrap(CustomQueueClient client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        client.bindQueueToExchange(); // veži se na exchange čim se app podigne
    }
}
