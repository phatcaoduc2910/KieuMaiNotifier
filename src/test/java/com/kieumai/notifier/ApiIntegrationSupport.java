package com.kieumai.notifier;

import com.kieumai.notifier.persistence.entity.*;
import com.kieumai.notifier.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class ApiIntegrationSupport {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired ReportRepository reports;
    @Autowired IncidentRepository incidents;
    @Autowired UserRepository users;
    @Autowired WardRepository wards;
    @Autowired ReportUpdateRepository updates;
    @Autowired JpaSubscriptionRepository subscriptions;
    @Autowired JdbcTemplate jdbc;
    String token;
    long wardA;
    long wardB;

    @BeforeEach
    void resetDatabase() throws Exception {
        updates.deleteAll();
        subscriptions.deleteAll();
        reports.deleteAll();
        incidents.deleteAll();
        users.deleteAll();
        wards.deleteAll();
        wardA = ward("TEST-A");
        wardB = ward("TEST-B");
        token = registerAndLogin("alice");
    }

    long ward(String code) {
        var ward = new WardEntity();
        ward.setCode(code);
        ward.setName(code);
        ward.setProvinceName("Test province");
        return wards.saveAndFlush(ward).getId();
    }

    String registerAndLogin(String username) throws Exception {
        assertEquals(201, request("POST", "/auth/register", null,
                Map.of("username", username, "password", "ExamplePass123!", "role", "USER")).statusCode());
        var response = request("POST", "/auth/login", null,
                Map.of("username", username, "password", "ExamplePass123!"));
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).get("token").asText();
    }

    Map<String, Object> validReport() {
        var body = new HashMap<String, Object>();
        body.put("wardId", wardA);
        body.put("type", "FLOOD");
        body.put("description", "Flood on the street");
        body.put("latitude", new BigDecimal("21.0285"));
        body.put("longitude", new BigDecimal("105.8048"));
        body.put("observedAt", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5).toString());
        return body;
    }

    JsonNode create(Map<String, Object> body) throws Exception {
        return create(body, token);
    }

    JsonNode create(Map<String, Object> body, String userToken) throws Exception {
        var response = request("POST", "/reports", userToken, body);
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    JsonNode get(String path) throws Exception {
        var response = request("GET", path, token, null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    IncidentEntity incident(long wardId, String type, String status, String latitude, String longitude) {
        var incident = new IncidentEntity();
        incident.setWardId(wardId);
        incident.setWard(wards.getReferenceById(wardId));
        incident.setType(type);
        incident.setTitle("Test incident");
        incident.setStatus(status);
        incident.setLatitude(new BigDecimal(latitude));
        incident.setLongitude(new BigDecimal(longitude));
        incident.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        incident.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        incident.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        return incidents.saveAndFlush(incident);
    }

    ReportEntity seedReport(IncidentEntity incident, String username, OffsetDateTime createdAt) {
        var report = new ReportEntity();
        report.setUser(users.findByUsername(username));
        report.setIncident(incident);
        report.setDescription("Flood on the street");
        report.setLatitude(incident.getLatitude());
        report.setLongitude(incident.getLongitude());
        report.setObservedAt(createdAt.minusMinutes(5));
        report.setCreatedAt(createdAt);
        return reports.saveAndFlush(report);
    }

    void assertError(HttpResponse<String> response, int status, String code) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(code, json.readTree(response.body()).get("code").asText());
    }

    HttpResponse<String> request(String method, String path, String userToken, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(30));
        if (userToken != null) builder.header("Authorization", "Bearer " + userToken);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
