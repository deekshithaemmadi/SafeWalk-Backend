package com.anurag.itds.controller;

import com.anurag.itds.service.MapsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public geocoding and routing proxy endpoints (paths unchanged for frontend compatibility).
 */
@RestController
public class RouteController {

    private final MapsService mapsService;

    public RouteController(MapsService mapsService) {
        this.mapsService = mapsService;
    }

    @GetMapping(value = "/api/geocode", produces = MediaType.APPLICATION_JSON_VALUE)
    public String geocode(@RequestParam String q) {
        return mapsService.geocode(q);
    }

    @GetMapping(value = "/api/route", produces = MediaType.APPLICATION_JSON_VALUE)
    public String route(@RequestParam Double startLat,
                        @RequestParam Double startLon,
                        @RequestParam Double endLat,
                        @RequestParam Double endLon) {
        return mapsService.route(startLat, startLon, endLat, endLon);
    }
}
