package com.queue_service.queue_service.customqueue;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryMessageQueue implements MessageQueue {

    private final ConcurrentMap<String, Deque<String>> queues = new ConcurrentHashMap<>();

    private Deque<String> getOrCreate(String name) {
        return queues.computeIfAbsent(name, k -> new ConcurrentLinkedDeque<>());
    }

    @Override
    public void createQueue(String name) {
        getOrCreate(name);
    }

    @Override
    public boolean deleteQueue(String name) {
        return queues.remove(name) != null;
    }

    @Override
    public boolean exists(String name) {
        return queues.containsKey(name);
    }

    @Override
    public void send(String queueName, String message) {
        getOrCreate(queueName).addLast(message);
    }

    @Override
    public String receive(String queueName) {
        Deque<String> q = getOrCreate(queueName);
        return q.pollFirst(); // non-blocking receive
    }

    @Override
    public List<String> list(String queueName) {
        Deque<String> q = getOrCreate(queueName);
        return List.copyOf(q);
    }

    @Override
    public Set<String> queues() {
        return Collections.unmodifiableSet(queues.keySet());
    }

    @Override
    public int size(String queueName) {
        return getOrCreate(queueName).size();
    }
}
