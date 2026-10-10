
package com.anurag.itds;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RouteController {

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    // =========================================================
    // GEOCODING
    // =========================================================

    @GetMapping(
        value = "/api/geocode",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String geocode(@RequestParam String q) {
        try {
            String encodedQuery = URLEncoder.encode(
                q, StandardCharsets.UTF_8
            );

            String url =
                "https://nominatim.openstreetmap.org/search"
                + "?format=json"
                + "&limit=5"
                + "&addressdetails=1"
                + "&dedupe=1"
                + "&countrycodes=in"
                + "&viewbox=77.0,19.0,81.5,15.8"
                + "&bounded=1"
                + "&q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("User-Agent", "SafeWalk-Prototype/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            System.out.println(
                "GEOCODE HTTP STATUS: " + response.statusCode()
            );

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {
                return response.body();
            }

            System.err.println(
                "GEOCODE ERROR BODY: " + response.body()
            );
            return "[]";

        } catch (Exception e) {
            System.err.println("GEOCODING ERROR:");
            e.printStackTrace();
            return "[]";
        }
    }

    // =========================================================
    // ROUTING
    // =========================================================

    @GetMapping(
        value = "/api/route",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String route(
        @RequestParam double startLat,
        @RequestParam double startLon,
        @RequestParam double endLat,
        @RequestParam double endLon
    ) {
        try {
            // OSRM coordinate order is longitude,latitude.
            String url =
                "https://router.project-osrm.org/route/v1/driving/"
                + startLon + "," + startLat
                + ";" + endLon + "," + endLat
                + "?overview=full"
                + "&geometries=geojson"
                + "&alternatives=true";

            System.out.println("OSRM REQUEST: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "SafeWalk-Prototype/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            System.out.println(
                "OSRM HTTP STATUS: " + response.statusCode()
            );

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {
                System.out.println("OSRM RESPONSE RECEIVED");
                return response.body();
            }

            System.err.println(
                "OSRM ERROR BODY: " + response.body()
            );

            return """
                {"code":"BackendRoutingError","routes":[]}
                """;

        } catch (Exception e) {
            System.err.println("ROUTING ERROR:");
            e.printStackTrace();

            return """
                {"code":"BackendRoutingError","routes":[]}
                """;
        }
    }
}