package com.queue_service.queue_service.customqueue;

import java.util.List;
import java.util.Set;

public interface MessageQueue {
    void createQueue(String name);
    boolean deleteQueue(String name);
    boolean exists(String name);
    void send(String queueName, String message);
    String receive(String queueName);
    List<String> list(String queueName);
    Set<String> queues();
    int size(String queueName);
}
