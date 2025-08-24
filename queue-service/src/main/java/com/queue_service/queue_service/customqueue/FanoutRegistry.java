package com.queue_service.queue_service.customqueue;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FanoutRegistry {

    // exchange -> (set queue-ova)
    private final Map<String, Set<String>> bindings = new ConcurrentHashMap<>();

    public void bind(String exchange, String queue) {
        bindings.computeIfAbsent(exchange, e -> ConcurrentHashMap.newKeySet()).add(queue);
    }

    public void unbind(String exchange, String queue) {
        Set<String> set = bindings.get(exchange);
        if (set != null) {
            set.remove(queue);
            if (set.isEmpty()) {
                bindings.remove(exchange);
            }
        }
    }

    /** Nepromenjiv set queue-ova vezanih na exchange. Ako nema exchange-a, vraća prazan set. */
    public Set<String> queues(String exchange) {
        Set<String> set = bindings.get(exchange);
        return (set == null) ? Set.of() : Collections.unmodifiableSet(set);
    }

    /** Snapshot svih exchange-ova i njihovih veza. */
    public Map<String, Set<String>> all() {
        Map<String, Set<String>> snap = new LinkedHashMap<>();
        bindings.forEach((ex, qs) -> snap.put(ex, Set.copyOf(qs)));
        return snap;
    }
}
