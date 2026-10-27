package com.kieumai.notifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ReportIntegrationTests extends ApiIntegrationSupport {
    @Test
    void firstReportCreatesPendingIncidentAndThirdActivatesTheSameIncident() throws Exception {
        long id = create(validReport()).get("incidentId").asLong();
        assertTrue(id > 0);
        assertEquals("PENDING", incidents.findById(id).orElseThrow().getStatus());
        assertEquals(id, create(validReport()).get("incidentId").asLong());
        assertEquals("PENDING", incidents.findById(id).orElseThrow().getStatus());
        assertEquals(id, create(validReport()).get("incidentId").asLong());
        assertEquals("ACTIVE", incidents.findById(id).orElseThrow().getStatus());
        for (int i = 0; i < 3; i++) assertEquals(id, create(validReport()).get("incidentId").asLong());
        assertEquals(1, incidents.count());
        assertEquals(6, get("/reports?incidentId=" + id).get("totalElements").asInt());
        assertEquals(6, get("/incidents/" + id).get("reportCount").asInt());
    }

    @Test
    void reportsExposeCoordinatesWithoutRemovedImageOrLocationFields() throws Exception {
        var created = create(validReport());
        var listed = get("/reports").get("items").get(0);
        assertEquals(created.get("id"), listed.get("id"));
        assertFalse(listed.has("imageUrls"));
        assertFalse(listed.has("locationDescription"));
        assertEquals(0, new BigDecimal("21.0285").compareTo(new BigDecimal(listed.get("latitude").asText())));
        assertEquals(0, new BigDecimal("105.8048").compareTo(new BigDecimal(listed.get("longitude").asText())));
        assertEquals(wardA, listed.get("wardId").asLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WARD", "TYPE", "DISTANCE"})
    void unrelatedReportsStartSeparatePendingIncidents(String difference) throws Exception {
        long firstId = create(validReport()).get("incidentId").asLong();
        create(validReport());
        var unrelated = validReport();
        if (difference.equals("WARD")) unrelated.put("wardId", wardB);
        if (difference.equals("TYPE")) unrelated.put("type", "OTHER");
        if (difference.equals("DISTANCE")) unrelated.put("latitude", 22);
        long otherId = create(unrelated).get("incidentId").asLong();
        assertNotEquals(firstId, otherId);
        assertEquals("PENDING", incidents.findById(firstId).orElseThrow().getStatus());
        assertEquals("PENDING", incidents.findById(otherId).orElseThrow().getStatus());
        assertEquals(firstId, create(validReport()).get("incidentId").asLong());
        assertEquals("ACTIVE", incidents.findById(firstId).orElseThrow().getStatus());
        assertEquals(2, incidents.count());
    }

    @Test
    void groupingUsesFiveHundredMeterRadiusAndOriginalCenter() throws Exception {
        var body = validReport();
        body.put("latitude", 0);
        body.put("longitude", 0);
        long originalId = create(body).get("incidentId").asLong();
        body.put("latitude", new BigDecimal("0.00449"));
        assertEquals(originalId, create(body).get("incidentId").asLong());
        body.put("latitude", new BigDecimal("0.00451"));
        assertNotEquals(originalId, create(body).get("incidentId").asLong());
        body.put("latitude", 0);
        assertEquals(originalId, create(body).get("incidentId").asLong());
        assertEquals("ACTIVE", incidents.findById(originalId).orElseThrow().getStatus());
        assertEquals(2, incidents.count());
    }

    @Test
    void deletedPendingReportDoesNotCountTowardActivation() throws Exception {
        var first = create(validReport());
        long id = first.get("incidentId").asLong();
        create(validReport());
        assertEquals(204, request("DELETE", "/reports/" + first.get("id").asLong(), token, null).statusCode());
        create(validReport());
        assertEquals("PENDING", incidents.findById(id).orElseThrow().getStatus());
        create(validReport());
        assertEquals("ACTIVE", incidents.findById(id).orElseThrow().getStatus());
        assertEquals(3, get("/reports?incidentId=" + id).get("totalElements").asInt());
    }

    @Test
    void softDeletionRequiresOwnershipAndDoesNotDowngradeActiveIncident() throws Exception {
        var first = create(validReport());
        create(validReport());
        long id = create(validReport()).get("incidentId").asLong();
        long reportId = first.get("id").asLong();
        var deadline = incidents.findById(id).orElseThrow().getExpiresAt();
        var bob = registerAndLogin("bob");
        assertError(request("DELETE", "/reports/" + reportId, bob, null), 403, "REPORT_NOT_OWNED");
        assertEquals(204, request("DELETE", "/reports/" + reportId, token, null).statusCode());
        assertNotNull(reports.findById(reportId).orElseThrow().getDeletedAt());
        assertEquals("ACTIVE", incidents.findById(id).orElseThrow().getStatus());
        assertEquals(deadline.toInstant(), incidents.findById(id).orElseThrow().getExpiresAt().toInstant());
        assertError(request("DELETE", "/reports/" + reportId, token, null), 404, "REPORT_NOT_FOUND");
        assertEquals(2, get("/reports").get("totalElements").asInt());
    }

    @Test
    void reportsFromDifferentAuthorsCountAndMyReportsCannotBeSpoofed() throws Exception {
        var first = create(validReport());
        var bob = registerAndLogin("bob");
        create(validReport(), bob);
        create(validReport(), registerAndLogin("carol"));
        long id = first.get("incidentId").asLong();
        assertEquals("ACTIVE", incidents.findById(id).orElseThrow().getStatus());
        var own = get("/me/reports?userId=" + users.findByUsername("bob").getId());
        assertEquals(1, own.get("totalElements").asInt());
        assertEquals(first.get("id"), own.get("items").get(0).get("id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "FAKE", "EXPIRED", "ELAPSED"})
    void closedOrExpiredIncidentDoesNotAcceptNewReports(String state) throws Exception {
        long oldId = create(validReport()).get("incidentId").asLong();
        var old = incidents.findById(oldId).orElseThrow();
        if (state.equals("ELAPSED")) old.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        else old.setStatus(state);
        incidents.saveAndFlush(old);
        long newId = create(validReport()).get("incidentId").asLong();
        assertNotEquals(oldId, newId);
        assertEquals("PENDING", incidents.findById(newId).orElseThrow().getStatus());
        assertEquals(newId, create(validReport()).get("incidentId").asLong());
        assertEquals(newId, create(validReport()).get("incidentId").asLong());
        assertEquals("ACTIVE", incidents.findById(newId).orElseThrow().getStatus());
    }

    @Test
    void subsequentReportExtendsIncidentLifetime() throws Exception {
        long id = create(validReport()).get("incidentId").asLong();
        var incident = incidents.findById(id).orElseThrow();
        incident.setExpiresAt(OffsetDateTime.now().plusMinutes(1));
        incidents.saveAndFlush(incident);
        assertEquals(id, create(validReport()).get("incidentId").asLong());
        assertTrue(incidents.findById(id).orElseThrow().getExpiresAt().isAfter(OffsetDateTime.now().plusHours(3)));
    }

    @Test
    void concurrentFirstReportsCreateOneActiveIncident() throws Exception {
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<java.net.http.HttpResponse<String>>>();
        try (var executor = Executors.newFixedThreadPool(6)) {
            for (int i = 0; i < 6; i++) futures.add(executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return request("POST", "/reports", token, validReport());
            }));
            start.countDown();
            for (var future : futures) {
                var response = future.get(30, TimeUnit.SECONDS);
                assertEquals(201, response.statusCode(), response.body());
            }
        }
        assertEquals(6, reports.count());
        assertEquals(1, incidents.count());
        assertEquals("ACTIVE", incidents.findAll().getFirst().getStatus());
        assertEquals(6, reports.countByIncidentIdAndDeletedAtIsNull(incidents.findAll().getFirst().getId()));
    }

    @Test
    void filtersCombineAndPaginationUsesStableOrder() throws Exception {
        var now = OffsetDateTime.now();
        var sameIncident = incident(wardA, "FLOOD", "ACTIVE", "21.0285", "105.8048");
        var first = seedReport(sameIncident, "alice", now.minusMinutes(2));
        var second = seedReport(sameIncident, "alice", now.minusMinutes(2));
        var third = seedReport(incident(wardB, "FLOOD", "ACTIVE", "21.0285", "105.8048"), "alice", now.minusMinutes(1));
        seedReport(incident(wardA, "OTHER", "ACTIVE", "21.0285", "105.8048"), "alice", now);
        var filtered = get("/reports?wardId=" + wardA + "&type=FLOOD&incidentId=" + sameIncident.getId());
        assertEquals(2, filtered.get("totalElements").asInt());
        assertEquals(second.getId().longValue(), filtered.get("items").get(0).get("id").asLong());
        assertEquals(first.getId().longValue(), filtered.get("items").get(1).get("id").asLong());
        var page = get("/reports?page=1&size=1");
        assertEquals(4, page.get("totalPages").asInt());
        assertEquals(third.getId().longValue(), page.get("items").get(0).get("id").asLong());
        assertEquals(0, get("/reports?page=99").get("items").size());
        assertEquals(0, get("/reports?wardId=999999").get("totalElements").asInt());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("wardId", -1), Arguments.of("wardId", null),
                Arguments.of("type", "UNKNOWN"), Arguments.of("type", " "),
                Arguments.of("description", " "), Arguments.of("description", "x".repeat(2001)),
                Arguments.of("latitude", 91), Arguments.of("latitude", -91), Arguments.of("latitude", null),
                Arguments.of("longitude", 181), Arguments.of("longitude", -181),
                Arguments.of("observedAt", "2099-01-01T00:00:00Z"),
                Arguments.of("observedAt", "2026-01-01T10:00:00"),
                Arguments.of("imageUrls", List.of("https://example.com/a.jpg")),
                Arguments.of("locationDescription", "Removed field"), Arguments.of("userId", 42));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidOrRemovedFieldsWithoutWritingData(String field, Object value) throws Exception {
        var body = validReport();
        body.put(field, value);
        assertError(request("POST", "/reports", token, body), 400, "VALIDATION_ERROR");
        assertEquals(0, reports.count());
        assertEquals(0, incidents.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "wardId=-1", "wardId=abc", "type=BAD", "incidentId=0"})
    void rejectsInvalidFilters(String query) throws Exception {
        assertError(request("GET", "/reports?" + query, token, null), 400, "VALIDATION_ERROR");
    }

    @Test
    void endpointsRequireAuthenticationAndRealReferences() throws Exception {
        assertEquals(401, request("POST", "/reports", null, validReport()).statusCode());
        assertEquals(401, request("GET", "/reports", null, null).statusCode());
        assertEquals(401, request("GET", "/me/reports", "invalid-token", null).statusCode());
        var invalidWard = validReport();
        invalidWard.put("wardId", 999999);
        assertError(request("POST", "/reports", token, invalidWard), 404, "WARD_NOT_FOUND");
        assertError(request("GET", "/reports?incidentId=999999", token, null), 404, "INCIDENT_NOT_FOUND");
        assertError(request("DELETE", "/reports/999999", token, null), 404, "REPORT_NOT_FOUND");
        assertError(request("DELETE", "/reports/0", token, null), 400, "VALIDATION_ERROR");
    }
}
