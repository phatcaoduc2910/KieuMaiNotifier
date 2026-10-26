package com.kieumai.notifier;

import com.kieumai.notifier.business.service.IncidentScheduler;
import com.kieumai.notifier.persistence.entity.IncidentEntity;
import com.kieumai.notifier.persistence.entity.User;
import com.kieumai.notifier.persistence.repository.IncidentConfirmationRepository;
import com.kieumai.notifier.persistence.repository.IncidentRepository;
import com.kieumai.notifier.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BetaIntegrationTests {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;
    @Autowired
    private IncidentRepository incidents;
    @Autowired
    private IncidentConfirmationRepository confirmations;
    @Autowired
    private IncidentScheduler scheduler;

    @BeforeEach
    void cleanDatabase() {
        confirmations.deleteAll();
        incidents.deleteAll();
        users.deleteAll();
    }

    @Test
    void registeredUserCanLogInAndConfirmIncidentUsingJwt() throws Exception {
        String token = registerAndLogin("alice");
        User alice = users.findByUserName("alice");
        IncidentEntity incident = incident("PENDING");

        assertEquals(200, request("POST", confirmationPath(incident), token, null).statusCode());
        assertTrue(confirmations.existsByIdIncidentIdAndIdUserId(incident.getId(), alice.getId()));

        var detail = request("GET", "/incidents/" + incident.getId(), token, null);
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains("\"confirmedByMe\":true"));
    }

    @Test
    void userIdQueryCannotImpersonateAnotherUser() throws Exception {
        String aliceToken = registerAndLogin("alice");
        registerAndLogin("bob");
        User alice = users.findByUserName("alice");
        User bob = users.findByUserName("bob");
        IncidentEntity incident = incident("PENDING");

        var response = request("POST", confirmationPath(incident) + "?userId=" + bob.getId(),
                aliceToken, null);

        assertEquals(200, response.statusCode());
        assertTrue(confirmations.existsByIdIncidentIdAndIdUserId(incident.getId(), alice.getId()));
        assertFalse(confirmations.existsByIdIncidentIdAndIdUserId(incident.getId(), bob.getId()));
    }

    @Test
    void duplicateConfirmationIsNotCountedTwiceAndCanBeWithdrawn() throws Exception {
        String token = registerAndLogin("alice");
        IncidentEntity incident = incident("PENDING");

        assertEquals(200, request("POST", confirmationPath(incident), token, null).statusCode());
        assertEquals(200, request("POST", confirmationPath(incident), token, null).statusCode());
        assertEquals(1, confirmations.countByIdIncidentId(incident.getId()));

        assertEquals(204, request("DELETE", confirmationPath(incident) + "/me", token, null).statusCode());
        assertEquals(0, confirmations.countByIdIncidentId(incident.getId()));
    }

    @Test
    void threeDifferentUsersActivatePendingIncident() throws Exception {
        IncidentEntity incident = incident("PENDING");
        for (String username : new String[]{"alice", "bob", "carol"}) {
            String token = registerAndLogin(username);
            assertEquals(200, request("POST", confirmationPath(incident), token, null).statusCode());
        }

        assertEquals(3, confirmations.countByIdIncidentId(incident.getId()));
        assertEquals("ACTIVE", incidents.findById(incident.getId()).orElseThrow().getStatus());
    }

    @Test
    void incidentListSupportsWardAndTypeFilters() throws Exception {
        String token = registerAndLogin("alice");
        incident("ACTIVE");

        var matching = request("GET", "/incidents?wardId=1&type=FLOOD", token, null);
        assertEquals(200, matching.statusCode());
        assertTrue(matching.body().contains("\"confirmationCount\":0"));
        assertEquals("[]", request("GET", "/incidents?wardId=2", token, null).body());
    }

    @Test
    void resolveIncidentWorksForAuthenticatedUserWithoutUserIdQuery() throws Exception {
        String token = registerAndLogin("alice");
        IncidentEntity incident = incident("ACTIVE");

        var response = request("POST", "/incidents/" + incident.getId() + "/updates", token,
                "{\"notes\":\"Water has receded\",\"imageUrl\":\"https://example.com/clear.jpg\"}");

        assertEquals(200, response.statusCode());
        assertEquals("RESOLVED", incidents.findById(incident.getId()).orElseThrow().getStatus());
    }

    @Test
    void schedulerExpiresActiveIncidentsWhoseDeadlineHasPassed() {
        IncidentEntity incident = incident("ACTIVE");
        incident.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        incidents.saveAndFlush(incident);

        scheduler.autoExpireIncidents();

        assertEquals("EXPIRED", incidents.findById(incident.getId()).orElseThrow().getStatus());
    }

    @Test
    void anonymousAndInvalidJwtRequestsCannotReadIncidents() throws Exception {
        assertEquals(401, request("GET", "/incidents", null, null).statusCode());
        assertEquals(401, request("GET", "/incidents", "invalid-token", null).statusCode());
    }

    @Test
    void duplicateRegistrationAndWrongPasswordAreRejected() throws Exception {
        registerAndLogin("alice");
        assertEquals(409, request("POST", "/auth/register", null, registration("alice")).statusCode());
        assertEquals(401, request("POST", "/auth/login", null,
                "{\"username\":\"alice\",\"password\":\"wrong-password\"}").statusCode());
    }

    private String registerAndLogin(String username) throws Exception {
        assertEquals(201, request("POST", "/auth/register", null, registration(username)).statusCode());
        var login = request("POST", "/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"ExamplePass123!\"}");
        assertEquals(200, login.statusCode());
        var matcher = TOKEN.matcher(login.body());
        assertTrue(matcher.find(), "Successful login must return a JWT");
        return matcher.group(1);
    }

    private String registration(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"ExamplePass123!\",\"role\":\"USER\"}";
    }

    private IncidentEntity incident(String status) {
        IncidentEntity incident = new IncidentEntity();
        incident.setWardId(1L);
        incident.setType("FLOOD");
        incident.setTitle("Integration test incident");
        incident.setLatitude(new BigDecimal("21.0285"));
        incident.setLongitude(new BigDecimal("105.8048"));
        incident.setStatus(status);
        incident.setCreatedAt(OffsetDateTime.now());
        incident.setExpiresAt(OffsetDateTime.now().plusHours(1));
        return incidents.saveAndFlush(incident);
    }

    private String confirmationPath(IncidentEntity incident) {
        return "/incidents/" + incident.getId() + "/confirmations";
    }

    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
