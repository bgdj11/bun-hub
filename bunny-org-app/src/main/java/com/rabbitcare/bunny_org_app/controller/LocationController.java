package com.rabbitcare.bunny_org_app.controller;

import com.rabbitcare.bunny_org_app.service.LocationSenderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/send")
public class LocationController {

    private final LocationSenderService service;

    public LocationController(LocationSenderService service) {
        this.service = service;
    }

    @PostMapping
    public String sendAll() {
        service.sendAllLocations();
        return "✅ Sve lokacije su poslate u queue.";
    }
}