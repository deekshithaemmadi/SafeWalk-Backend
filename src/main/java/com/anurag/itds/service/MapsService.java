package com.anurag.itds.service;

import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ExternalServiceException;
import com.anurag.itds.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Thin proxy over OpenStreetMap Nominatim (geocoding) and OSRM (routing).
 *
 * <p>Successful upstream JSON is passed through unchanged so existing frontend
 * parsing keeps working. Invalid input is rejected before any network call, and
 * upstream failures are mapped to clear 4xx/5xx errors instead of fake empty
 * results. Search text and coordinates are never logged (location is personal data).</p>
 */
@Service
public class MapsService {

    public static final String ROUTING_ERROR_CODE = "BackendRoutingError";
    /** Telangana bounding box (lon_min,lat_max,lon_max,lat_min) used to bias geocoding. */
    static final String TELANGANA_VIEWBOX = "77.0,19.0,81.5,15.8";
    static final int MIN_QUERY_LENGTH = 2;
    static final int MAX_QUERY_LENGTH = 200;

    private static final Logger log = LoggerFactory.getLogger(MapsService.class);

    private final HttpClient httpClient;
    private final String nominatimBaseUrl;
    private final String osrmBaseUrl;
    private final String userAgent;
    private final Duration readTimeout;
    private final String countryCodes;

    public MapsService(@Value("${safewalk.maps.nominatim-base-url}") String nominatimBaseUrl,
                       @Value("${safewalk.maps.osrm-base-url}") String osrmBaseUrl,
                       @Value("${safewalk.maps.user-agent:SafeWalkBackend/1.0}") String userAgent,
                       @Value("${safewalk.maps.connect-timeout-ms:5000}") long connectTimeoutMs,
                       @Value("${safewalk.maps.read-timeout-ms:10000}") long readTimeoutMs,
                       @Value("${safewalk.maps.geocode-country-codes:in}") String countryCodes) {
        this.nominatimBaseUrl = stripTrailingSlash(nominatimBaseUrl);
        this.osrmBaseUrl = stripTrailingSlash(osrmBaseUrl);
        this.userAgent = userAgent;
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        this.countryCodes = countryCodes;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Returns the raw Nominatim JSON array for {@code query}, restricted to Telangana. */
    public String geocode(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < MIN_QUERY_LENGTH) {
            throw new BadRequestException("Search text 'q' must be at least " + MIN_QUERY_LENGTH + " characters.");
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Search text 'q' must be at most " + MAX_QUERY_LENGTH + " characters.");
        }
        String url = nominatimBaseUrl + "/search?format=json&limit=5&addressdetails=1&dedupe=1"
                + "&countrycodes=" + URLEncoder.encode(countryCodes, StandardCharsets.UTF_8)
                + "&viewbox=" + TELANGANA_VIEWBOX + "&bounded=1"
                + "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);

        HttpResponse<String> response = send(url, "Geocoding", null);
        if (isSuccess(response.statusCode())) {
            return response.body();
        }
        log.warn("Geocoding upstream returned HTTP {}", response.statusCode());
        throw new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                "Geocoding service returned an error. Please try again later.", null);
    }

    /** Returns the raw OSRM route JSON (GeoJSON geometry, alternatives) between two points. */
    public String route(Double startLat, Double startLon, Double endLat, Double endLon) {
        validateLatitude("startLat", startLat);
        validateLongitude("startLon", startLon);
        validateLatitude("endLat", endLat);
        validateLongitude("endLon", endLon);
        if (startLat.equals(endLat) && startLon.equals(endLon)) {
            throw new BadRequestException("Start and end locations must be different.");
        }
        // OSRM coordinate order is longitude,latitude.
        String coordinates = format(startLon) + "," + format(startLat) + ";" + format(endLon) + "," + format(endLat);
        String url = osrmBaseUrl + "/route/v1/driving/" + coordinates
                + "?overview=full&geometries=geojson&alternatives=true";

        HttpResponse<String> response = send(url, "Routing", ROUTING_ERROR_CODE);
        int status = response.statusCode();
        if (isSuccess(status)) {
            return response.body();
        }
        String body = response.body() == null ? "" : response.body();
        if (status == 400 && body.contains("\"NoRoute\"")) {
            throw new ResourceNotFoundException("No route could be found between the selected locations.");
        }
        log.warn("Routing upstream returned HTTP {}", status);
        throw new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                "Routing service returned an error. Please try again later.", ROUTING_ERROR_CODE);
    }

    private HttpResponse<String> send(String url, String serviceName, String compatCode) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(readTimeout)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException ex) {
            log.warn("{} upstream timed out", serviceName);
            throw new ExternalServiceException(HttpStatus.GATEWAY_TIMEOUT,
                    serviceName + " service timed out. Please try again.", compatCode);
        } catch (IOException ex) {
            log.warn("{} upstream unreachable: {}", serviceName, ex.getClass().getSimpleName());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    serviceName + " service is currently unavailable. Please try again later.", compatCode);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    serviceName + " request was interrupted.", compatCode);
        }
    }

    private static void validateLatitude(String name, Double value) {
        if (value == null || !Double.isFinite(value) || value < -90 || value > 90) {
            throw new BadRequestException("'" + name + "' must be a latitude between -90 and 90.");
        }
    }

    private static void validateLongitude(String name, Double value) {
        if (value == null || !Double.isFinite(value) || value < -180 || value > 180) {
            throw new BadRequestException("'" + name + "' must be a longitude between -180 and 180.");
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
