package com.kieumai.notifier;

import com.kieumai.notifier.business.service.IncidentScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BetaIntegrationTests extends ApiIntegrationSupport {
    @Autowired IncidentScheduler scheduler;

    @Test
    void registrationStoresHashAndServerTimestampWithoutAnAdminRole() {
        var user = users.findByUsername("alice");
        assertTrue(user.getPasswordHash().startsWith("{bcrypt}"));
        assertNotEquals("ExamplePass123!", user.getPasswordHash());
        assertEquals("alice", user.getDisplayName());
        assertNotNull(user.getCreatedAt());
        assertNull(user.getLatitude());
        assertNull(user.getLongitude());
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertFalse(payload.contains("ADMIN"));
    }

    @Test
    void duplicateRegistrationWrongPasswordAndAnonymousRequestsAreRejected() throws Exception {
        assertEquals(409, request("POST", "/auth/register", null,
                Map.of("username", "alice", "password", "ExamplePass123!")).statusCode());
        assertEquals(401, request("POST", "/auth/login", null,
                Map.of("username", "alice", "password", "wrong-password")).statusCode());
        assertEquals(401, request("GET", "/incidents", null, null).statusCode());
    }

    @Test
    void schedulerExpiresPendingAndActiveIncidentsButKeepsFutureOnes() {
        for (String status : List.of("PENDING", "ACTIVE", "ALERTED")) {
            var expired = incident(wardA, "FLOOD", status, "0", "0");
            expired.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
            incidents.saveAndFlush(expired);
        }
        var future = incident(wardA, "FLOOD", "ACTIVE", "0", "0");
        scheduler.autoExpireIncidents();
        assertEquals(3, incidents.findAll().stream().filter(i -> i.getStatus().equals("EXPIRED")).count());
        assertEquals("ACTIVE", incidents.findById(future.getId()).orElseThrow().getStatus());
    }

    @Test
    void incidentDetailCountsReportsAndExistingFiltersStillWork() throws Exception {
        long id = create(validReport()).get("incidentId").asLong();
        var detail = get("/incidents/" + id);
        assertEquals(1, detail.get("reportCount").asInt());
        assertFalse(detail.has("confirmationCount"));
        assertFalse(detail.has("confirmedByMe"));
        assertEquals(200, request("POST", "/incidents/" + id + "/updates", token, null).statusCode());
        assertEquals("RESOLVED", incidents.findById(id).orElseThrow().getStatus());
        assertEquals(1, get("/incidents?wardId=" + wardA + "&type=FLOOD").size());
        assertEquals(0, get("/incidents?wardId=" + wardB).size());
    }
}
