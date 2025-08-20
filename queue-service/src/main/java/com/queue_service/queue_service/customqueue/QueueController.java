package com.queue_service.queue_service.customqueue;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private final InMemoryMessageQueue queue;

    public QueueController(InMemoryMessageQueue queue) {
        this.queue = queue;
    }

    // Create queue (idempotent)
    @PutMapping("/{name}")
    public ResponseEntity<?> create(@PathVariable String name) {
        queue.createQueue(name);
        return ResponseEntity.ok(Map.of("queue", name, "status", "ready"));
    }

    // Delete queue
    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        boolean removed = queue.deleteQueue(name);
        return ResponseEntity.status(removed ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(Map.of("queue", name, "deleted", removed));
    }

    // List queues
    @GetMapping
    public ResponseEntity<Set<String>> queues() {
        return ResponseEntity.ok(queue.queues());
    }

    // Send message (body is raw JSON/text)
    @PostMapping(
            path = "/{name}/send",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE }
    )
    public ResponseEntity<?> send(@PathVariable String name, @RequestBody String payload) {
        queue.send(name, payload);
        System.out.println("📩 [QueueService] primljeno u '" + name + "': " + payload);
        return ResponseEntity.ok(Map.of("queue", name, "enqueued", true, "size", queue.size(name)));
    }

    // Receive next message (non-blocking)
    @GetMapping("/{name}/receive")
    public ResponseEntity<?> receive(@PathVariable String name) {
        String msg = queue.receive(name);
        if (msg == null){
            System.out.println("⚠️ [QueueService] '" + name + "' je prazan.");
            return ResponseEntity.noContent().build();
        }
        System.out.println("📤 [QueueService] vraćam iz '" + name + "': " + msg);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(msg);
    }

    // Peek all (debug)
    @GetMapping("/{name}")
    public ResponseEntity<List<String>> list(@PathVariable String name) {
        return ResponseEntity.ok(queue.list(name));
    }
}
