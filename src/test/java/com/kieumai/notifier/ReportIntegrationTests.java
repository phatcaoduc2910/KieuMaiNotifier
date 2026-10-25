package com.kieumai.notifier;

import com.kieumai.notifier.persistence.entity.IncidentConfirmationEntity;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.ReportEntity;
import com.kieumai.notifier.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:report-integration;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=false"
})
class ReportIntegrationTests {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired private ObjectMapper json;
    @Autowired private ReportRepository reports;
    @Autowired private IncidentRepository incidents;
    @Autowired private IncidentConfirmationRepository confirmations;
    @Autowired private UserRepository users;
    private String aliceToken;

    @BeforeEach
    void setUp() throws Exception {
        reports.deleteAll();
        confirmations.deleteAll();
        incidents.deleteAll();
        users.deleteAll();
        aliceToken = registerAndLogin("alice");
    }

    @Test
    void creationExplicitlyWaitsForIncidentIntegrationWithoutWritingData() throws Exception {
        var existing = seedIncident(1, "FLOOD");
        var deadline = incidents.findById(existing.getId()).orElseThrow().getExpiresAt();
        assertError(request("POST", "/reports", aliceToken, validReport()), 501, "INCIDENT_INTEGRATION_PENDING");
        assertEquals(0, reports.count());
        assertEquals(1, incidents.count());
        assertEquals(0, confirmations.count());
        var unchanged = incidents.findById(existing.getId()).orElseThrow();
        assertEquals("ACTIVE", unchanged.getStatus());
        assertEquals(deadline.toInstant(), unchanged.getExpiresAt().toInstant());
    }

    @Test
    void listsPersistedReportDataWithImagesAndCoordinates() throws Exception {
        var report = seedReport(seedIncident(1, "FLOOD"), "alice", OffsetDateTime.now(ZoneOffset.UTC));
        var listed = get("/reports").get("items").get(0);
        assertEquals(report.getId().longValue(), listed.get("id").asLong());
        assertEquals(users.findByUserName("alice").getId().longValue(), listed.get("userId").asLong());
        assertEquals("Near the school", listed.get("locationDescription").asText());
        assertEquals("https://example.com/one.jpg", listed.get("imageUrls").get(0).asText());
        assertEquals("https://example.com/two.jpg", listed.get("imageUrls").get(1).asText());
        assertEquals(0, new BigDecimal(listed.get("latitude").asText()).compareTo(new BigDecimal("21.0285")));
        assertEquals(0, new BigDecimal(listed.get("longitude").asText()).compareTo(new BigDecimal("105.8048")));
        assertTrue(listed.get("observedAt").asText().endsWith("Z"));
    }

    @Test
    void filtersCombineWithAndAndPagesUseStableDescendingOrder() throws Exception {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var incident = seedIncident(1, "FLOOD");
        var first = seedReport(incident, "alice", now.minusMinutes(2));
        var second = seedReport(incident, "alice", now.minusMinutes(2));
        var third = seedReport(seedIncident(2, "FLOOD"), "alice", now.minusMinutes(1));
        seedReport(seedIncident(1, "OTHER"), "alice", now);

        var filtered = get("/reports?wardId=1&type=FLOOD&incidentId=" + incident.getId());
        assertEquals(2, filtered.get("totalElements").asInt());
        assertEquals(second.getId().longValue(), filtered.get("items").get(0).get("id").asLong());
        assertEquals(first.getId().longValue(), filtered.get("items").get(1).get("id").asLong());
        assertEquals(0, get("/reports?wardId=2&incidentId=" + incident.getId()).get("totalElements").asInt());
        var page = get("/reports?page=1&size=1");
        assertEquals(1, page.get("page").asInt());
        assertEquals(1, page.get("size").asInt());
        assertEquals(4, page.get("totalPages").asInt());
        assertEquals(third.getId().longValue(), page.get("items").get(0).get("id").asLong());
        assertEquals(0, get("/reports?page=99").get("items").size());
        assertEquals(0, get("/reports?wardId=9999").get("totalElements").asInt());
    }

    @Test
    void myReportsUsesTokenIdentityEvenWithUserIdQuery() throws Exception {
        var incident = seedIncident(1, "FLOOD");
        var alice = seedReport(incident, "alice", OffsetDateTime.now());
        registerAndLogin("bob");
        seedReport(incident, "bob", OffsetDateTime.now());
        var own = get("/me/reports?userId=" + users.findByUserName("bob").getId());
        assertEquals(1, own.get("totalElements").asInt());
        assertEquals(alice.getId().longValue(), own.get("items").get(0).get("id").asLong());
        assertEquals(0, get("/me/reports?type=OTHER").get("totalElements").asInt());
    }

    @Test
    void deletingRequiresOwnershipAndPreservesIncidentAndConfirmations() throws Exception {
        var incident = seedIncident(1, "FLOOD");
        var first = seedReport(incident, "alice", OffsetDateTime.now());
        var second = seedReport(incident, "alice", OffsetDateTime.now());
        long incidentId = incident.getId();
        long reportId = first.getId();
        confirmations.saveAndFlush(new IncidentConfirmationEntity(incident, users.findByUserName("alice")));
        var deadline = incidents.findById(incidentId).orElseThrow().getExpiresAt();
        String bobToken = registerAndLogin("bob");
        assertError(request("DELETE", "/reports/" + reportId, bobToken, null), 403, "REPORT_NOT_OWNED");
        var deleted = request("DELETE", "/reports/" + reportId, aliceToken, null);
        assertEquals(204, deleted.statusCode());
        assertEquals("", deleted.body());
        assertNotNull(reports.findById(reportId).orElseThrow().getDeletedAt());
        assertEquals(deadline.toInstant(), incidents.findById(incidentId).orElseThrow().getExpiresAt().toInstant());
        assertEquals(1, confirmations.countByIdIncidentId(incidentId));
        assertEquals(second.getId().longValue(), get("/reports").get("items").get(0).get("id").asLong());
        assertEquals(1, get("/me/reports").get("totalElements").asInt());
        assertError(request("DELETE", "/reports/" + reportId, aliceToken, null), 404, "REPORT_NOT_FOUND");
        assertEquals(204, request("DELETE", "/reports/" + second.getId(), aliceToken, null).statusCode());
        assertEquals(1, incidents.count());
        var empty = get("/reports");
        assertEquals(0, empty.get("totalElements").asInt());
        assertEquals(0, empty.get("totalPages").asInt());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("wardId", -1), Arguments.of("wardId", null),
                Arguments.of("type", "UNKNOWN"), Arguments.of("type", " "),
                Arguments.of("description", " "), Arguments.of("description", "x".repeat(2001)),
                Arguments.of("latitude", 91), Arguments.of("latitude", -91), Arguments.of("latitude", null),
                Arguments.of("longitude", 181), Arguments.of("longitude", -181),
                Arguments.of("observedAt", "2099-01-01T00:00:00Z"), Arguments.of("observedAt", "2026-01-01T10:00:00"),
                Arguments.of("imageUrls", List.of("http://example.com/image.jpg")),
                Arguments.of("imageUrls", List.of("https://")), Arguments.of("imageUrls", null),
                Arguments.of("imageUrls", java.util.Collections.nCopies(6, "https://example.com/image.jpg")),
                Arguments.of("locationDescription", "x".repeat(501)), Arguments.of("userId", 42));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidBodyBeforePendingIntegration(String field, Object value) throws Exception {
        Map<String, Object> body = validReport();
        body.put(field, value);
        assertError(request("POST", "/reports", aliceToken, body), 400, "VALIDATION_ERROR");
        assertEquals(0, reports.count());
        assertEquals(0, incidents.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "wardId=-1", "wardId=abc", "type=BAD", "incidentId=0"})
    void rejectsInvalidFilters(String query) throws Exception {
        assertError(request("GET", "/reports?" + query, aliceToken, null), 400, "VALIDATION_ERROR");
    }

    @Test
    void unknownReferencesReturnNotFound() throws Exception {
        assertError(request("GET", "/reports?incidentId=9999", aliceToken, null), 404, "INCIDENT_NOT_FOUND");
        assertError(request("DELETE", "/reports/9999", aliceToken, null), 404, "REPORT_NOT_FOUND");
        assertError(request("DELETE", "/reports/0", aliceToken, null), 400, "VALIDATION_ERROR");
    }

    @Test
    void reportEndpointsRequireAuthentication() throws Exception {
        assertEquals(401, request("POST", "/reports", null, validReport()).statusCode());
        assertEquals(401, request("GET", "/reports", null, null).statusCode());
        assertEquals(401, request("GET", "/me/reports", null, null).statusCode());
        assertEquals(401, request("DELETE", "/reports/1", null, null).statusCode());
        assertEquals(401, request("POST", "/reports", "invalid-token", validReport()).statusCode());
        assertEquals(0, reports.count());
    }

    @Test
    void optionalImagesCanBeOmittedBeforePendingIntegration() throws Exception {
        var body = validReport();
        body.remove("imageUrls");
        body.remove("locationDescription");
        assertError(request("POST", "/reports", aliceToken, body), 501, "INCIDENT_INTEGRATION_PENDING");
    }

    private Map<String, Object> validReport() {
        var body = new HashMap<String, Object>();
        body.put("wardId", 1);
        body.put("type", "FLOOD");
        body.put("description", "Flood on the street");
        body.put("latitude", new BigDecimal("21.0285"));
        body.put("longitude", new BigDecimal("105.8048"));
        body.put("observedAt", OffsetDateTime.now().minusMinutes(5).toString());
        body.put("locationDescription", "Near the school");
        body.put("imageUrls", List.of("https://example.com/one.jpg", "https://example.com/two.jpg"));
        return body;
    }

    private IncidentEntity seedIncident(long wardId, String type) {
        var incident = new IncidentEntity();
        incident.setWardId(wardId);
        incident.setType(type);
        incident.setStatus("ACTIVE");
        incident.setTitle("Existing incident fixture");
        incident.setLatitude(new BigDecimal("21.0285"));
        incident.setLongitude(new BigDecimal("105.8048"));
        incident.setCreatedAt(OffsetDateTime.now().minusHours(1));
        incident.setExpiresAt(OffsetDateTime.now().plusHours(1));
        return incidents.saveAndFlush(incident);
    }

    private ReportEntity seedReport(IncidentEntity incident, String username, OffsetDateTime createdAt) {
        var report = new ReportEntity();
        report.setUser(users.findByUserName(username));
        report.setIncident(incident);
        report.setDescription("Flood on the street");
        report.setLocationDescription("Near the school");
        report.setImageUrls(new java.util.ArrayList<>(List.of("https://example.com/one.jpg", "https://example.com/two.jpg")));
        report.setLatitude(new BigDecimal("21.0285"));
        report.setLongitude(new BigDecimal("105.8048"));
        report.setObservedAt(createdAt.minusMinutes(5));
        report.setCreatedAt(createdAt);
        return reports.saveAndFlush(report);
    }

    private String registerAndLogin(String username) throws Exception {
        assertEquals(201, request("POST", "/auth/register", null,
                Map.of("username", username, "password", "ExamplePass123!", "role", "USER")).statusCode());
        var response = request("POST", "/auth/login", null,
                Map.of("username", username, "password", "ExamplePass123!"));
        assertEquals(200, response.statusCode());
        return json.readTree(response.body()).get("token").asText();
    }

    private JsonNode get(String path) throws Exception {
        var response = request("GET", path, aliceToken, null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private void assertError(HttpResponse<String> response, int status, String code) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(code, json.readTree(response.body()).get("code").asText());
    }

    private HttpResponse<String> request(String method, String path, String token, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(java.time.Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
