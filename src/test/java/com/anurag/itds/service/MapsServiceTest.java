package com.anurag.itds.service;

import com.anurag.itds.exception.ApiException;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ExternalServiceException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the real HTTP client against a local stub server (no internet needed). */
class MapsServiceTest {

    private HttpServer server;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastUserAgent = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String body = "[]";
    private volatile long delayMs = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            lastUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            } catch (IOException ignored) {
                // client gave up (timeout test)
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private MapsService service(long readTimeoutMs) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        return new MapsService(base, base + "/", "SafeWalkTest/1.0", 1000, readTimeoutMs, "in");
    }

    @Test
    void geocodePassesThroughUpstreamJsonAndEncodesQuery() {
        body = "[{\"display_name\":\"Charminar, Hyderabad\",\"lat\":\"17.36\",\"lon\":\"78.47\"}]";
        String result = service(2000).geocode("  Charminar & Co ");
        assertThat(result).isEqualTo(body);
        assertThat(lastPath.get()).isEqualTo("/search");
        assertThat(lastQuery.get()).contains("q=Charminar+%26+Co").contains("countrycodes=in").contains("bounded=1");
        assertThat(lastUserAgent.get()).isEqualTo("SafeWalkTest/1.0");
    }

    @Test
    void geocodeRejectsBlankShortAndOverlongQueriesWithoutCallingUpstream() {
        MapsService maps = service(2000);
        assertThatThrownBy(() -> maps.geocode(null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> maps.geocode("   ")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> maps.geocode("a")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> maps.geocode("x".repeat(201))).isInstanceOf(BadRequestException.class);
        assertThat(lastPath.get()).isNull();
    }

    @Test
    void geocodeUpstreamErrorBecomesBadGateway() {
        status = 503;
        body = "{\"error\":\"overloaded\"}";
        assertThatThrownBy(() -> service(2000).geocode("Hitech City"))
                .isInstanceOf(ExternalServiceException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void routeUsesLonLatOrderAndPassesThroughJson() {
        body = "{\"code\":\"Ok\",\"routes\":[{\"distance\":1234.5}]}";
        String result = service(2000).route(17.385, 78.4867, 17.4401, 78.3489);
        assertThat(result).isEqualTo(body);
        assertThat(lastPath.get()).isEqualTo("/route/v1/driving/78.486700,17.385000;78.348900,17.440100");
        assertThat(lastQuery.get()).contains("geometries=geojson").contains("alternatives=true");
    }

    @Test
    void routeValidatesCoordinates() {
        MapsService maps = service(2000);
        assertThatThrownBy(() -> maps.route(91.0, 78.0, 17.0, 78.0)).hasMessageContaining("startLat");
        assertThatThrownBy(() -> maps.route(17.0, -181.0, 17.0, 78.0)).hasMessageContaining("startLon");
        assertThatThrownBy(() -> maps.route(17.0, 78.0, Double.NaN, 78.0)).hasMessageContaining("endLat");
        assertThatThrownBy(() -> maps.route(17.0, 78.0, 17.1, null)).hasMessageContaining("endLon");
        assertThatThrownBy(() -> maps.route(17.0, 78.0, 17.0, 78.0))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("different");
        assertThat(lastPath.get()).isNull();
    }

    @Test
    void routeNoRouteBecomesNotFound() {
        status = 400;
        body = "{\"code\":\"NoRoute\",\"message\":\"Impossible route\"}";
        assertThatThrownBy(() -> service(2000).route(17.0, 78.0, 17.1, 78.1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void routeUpstreamFailureCarriesCompatCode() {
        status = 500;
        assertThatThrownBy(() -> service(2000).route(17.0, 78.0, 17.1, 78.1))
                .isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getCompatCode()).isEqualTo(MapsService.ROUTING_ERROR_CODE);
                });
    }

    @Test
    void slowUpstreamBecomesGatewayTimeout() {
        delayMs = 1500;
        assertThatThrownBy(() -> service(300).route(17.0, 78.0, 17.1, 78.1))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void unreachableUpstreamBecomesServiceUnavailable() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        String base = "http://127.0.0.1:" + freePort;
        MapsService maps = new MapsService(base, base, "ua", 500, 500, "in");
        assertThatThrownBy(() -> maps.geocode("Gachibowli"))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
