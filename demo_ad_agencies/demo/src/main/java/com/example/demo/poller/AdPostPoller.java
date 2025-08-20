package com.example.demo.poller;

import com.example.demo.customqueue.CustomQueueClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdPostPoller {

    private final CustomQueueClient queueClient;

    public AdPostPoller(CustomQueueClient queueClient) {
        this.queueClient = queueClient;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollQueue() {
        String message = queueClient.receiveMessage();
        if (message != null) {
            System.out.println("✅ Primljena poruka: " + message);
        }
    }
}
