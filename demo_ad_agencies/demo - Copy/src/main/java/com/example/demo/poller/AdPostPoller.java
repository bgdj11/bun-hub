package com.example.demo.poller;

import com.example.demo.customqueue.CustomQueueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdPostPoller {

    private final CustomQueueClient queueClient;

    public AdPostPoller(CustomQueueClient queueClient) {
        this.queueClient = queueClient;
    }

    // ili @Scheduled(fixedDelayString = "${poll.interval.ms:5000}")
    @Scheduled(fixedDelayString = "${poll.interval.ms:5000}")
    public void pollQueue() {
        String message = queueClient.receiveMessage();
        if (message != null) {
            System.out.println("✅ Primljena poruka (fanout): " + message);
            // ovde radiš parse i dalje šta treba
        }
    }
}
