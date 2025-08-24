package com.queue_service.queue_service.customqueue;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/exchange")
public class ExchangeController {

    private final FanoutRegistry fanout;
    private final InMemoryMessageQueue queueStore;

    public ExchangeController(FanoutRegistry fanout, InMemoryMessageQueue queueStore) {
        this.fanout = fanout;
        this.queueStore = queueStore;
    }

    // Bind: exchange -> queue
    @PutMapping("/{ex}/bind/{queue}")
    public Map<String, Object> bind(@PathVariable String ex, @PathVariable String queue) {
        // opcionalno: kreiraj queue ako ne postoji
        queueStore.createQueue(queue);
        fanout.bind(ex, queue);
        return Map.of(
                "exchange", ex,
                "boundQueue", queue,
                "queues", fanout.queues(ex),
                "ok", true
        );
    }

    // Unbind
    @DeleteMapping("/{ex}/bind/{queue}")
    public Map<String, Object> unbind(@PathVariable String ex, @PathVariable String queue) {
        fanout.unbind(ex, queue);
        return Map.of(
                "exchange", ex,
                "unboundQueue", queue,
                "queues", fanout.queues(ex),
                "ok", true
        );
    }

    // List queues for one exchange
    @GetMapping("/{ex}")
    public Set<String> list(@PathVariable String ex) {
        return fanout.queues(ex);
    }

    // List all exchanges (korisno za debug)
    @GetMapping
    public Map<String, Set<String>> listAll() {
        return fanout.all();
    }

    // Publish to exchange (fanout u sve vezane queue-ove)
    @PostMapping(
            path = "/{ex}/publish",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE }
    )
    public ResponseEntity<Map<String, Object>> publish(@PathVariable String ex,
                                                       @RequestBody String payload) {

        Set<String> targets = fanout.queues(ex);
        int delivered = 0;
        List<Map<String, Object>> perQueue = new ArrayList<>();

        for (String q : targets) {
            queueStore.send(q, payload);
            delivered++;
            perQueue.add(Map.of(
                    "queue", q,
                    "size", queueStore.size(q)
            ));
            System.out.println("📣 [Fanout] ex='" + ex + "' → queue='" + q + "' payload=" + payload);
        }

        Map<String, Object> body = Map.of(
                "exchange", ex,
                "queuesAffected", targets,
                "delivered", delivered,
                "details", perQueue
        );

        // Ako nema vezanih queue-ova, vrati 202 Accepted (nije greška, ali niko nije dobio poruku)
        return targets.isEmpty()
                ? ResponseEntity.accepted().body(body)
                : ResponseEntity.ok(body);
    }
}
