package com.kieumai.notifier;

import com.kieumai.notifier.persistence.entity.IncidentEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifierIntegrationTests extends ApiIntegrationSupport {

    @Test
    void userCanSubscribeListAndUnsubscribe() throws Exception {
        var created = request("POST", "/me/subscriptions", token, Map.of("wardId", wardA));
        assertEquals(201, created.statusCode(), created.body());
        assertEquals(wardA, json.readTree(created.body()).get("wardId").asLong());
        assertFalse(created.body().contains("userId"));

        var list = get("/me/subscriptions");
        assertEquals(1, list.size());
        assertEquals(wardA, list.get(0).get("wardId").asLong());

        assertEquals(204, request("DELETE", "/me/subscriptions/" + wardA, token, null).statusCode());
        assertEquals(0, get("/me/subscriptions").size());
    }

    @Test
    void subscriptionErrorsUseTheDocumentedStatusCodes() throws Exception {
        assertEquals(201, request("POST", "/me/subscriptions", token, Map.of("wardId", wardA)).statusCode());

        assertError(request("POST", "/me/subscriptions", token, Map.of("wardId", wardA)),
                409, "ALREADY_SUBSCRIBED");
        assertError(request("POST", "/me/subscriptions", token, Map.of("wardId", 999999)),
                404, "WARD_NOT_FOUND");
        assertError(request("POST", "/me/subscriptions", token, Map.of()), 400, "VALIDATION_ERROR");
        assertError(request("POST", "/me/subscriptions", token, Map.of("wardId", -1)), 400, "VALIDATION_ERROR");
        assertError(request("DELETE", "/me/subscriptions/" + wardB, token, null), 404, "SUBSCRIPTION_NOT_FOUND");
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        assertEquals(401, request("GET", "/me/subscriptions", null, null).statusCode());
        assertEquals(401, request("POST", "/me/subscriptions", null, Map.of("wardId", wardA)).statusCode());
        assertEquals(401, request("GET", "/me/feed", null, null).statusCode());
        assertEquals(401, request("GET", "/wards", null, null).statusCode());
    }

    @Test
    void feedShowsOnlyActiveIncidentsOfFollowedWards() throws Exception {
        assertEquals(201, request("POST", "/me/subscriptions", token, Map.of("wardId", wardA)).statusCode());
        IncidentEntity active = incident(wardA, "FLOOD", "ACTIVE", "21.0285", "105.8048");
        incident(wardA, "FLOOD", "PENDING", "21.0285", "105.8048");
        incident(wardB, "FLOOD", "ACTIVE", "21.0285", "105.8048");

        var feed = get("/me/feed");
        assertEquals(1, feed.size(), feed.toString());
        assertEquals(active.getId(), feed.get(0).get("incidentId").asLong());

        String otherToken = registerAndLogin("bob");
        var otherFeed = request("GET", "/me/feed", otherToken, null);
        assertEquals(200, otherFeed.statusCode());
        assertEquals(0, json.readTree(otherFeed.body()).size());
    }

    @Test
    void wardsAreListedSoClientCanPickOneToFollow() throws Exception {
        var wardList = get("/wards");

        assertEquals(2, wardList.size());
        assertEquals(wardA, wardList.get(0).get("id").asLong());
        assertEquals("TEST-A", wardList.get(0).get("code").asText());
        assertEquals("Test province", wardList.get(0).get("provinceName").asText());
        assertEquals(wardB, wardList.get(1).get("id").asLong());
    }
}
