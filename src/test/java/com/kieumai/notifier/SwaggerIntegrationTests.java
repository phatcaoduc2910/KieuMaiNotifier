package com.kieumai.notifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwaggerIntegrationTests extends ApiIntegrationSupport {
    @Test
    void swaggerUiIsAccessibleWithoutSigningIn() throws Exception {
        var response = request("GET", "/swagger-ui/index.html", null, null);
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("Swagger UI"));
    }

    @Test
    void swaggerConfigurationIsAccessibleWithoutSigningIn() throws Exception {
        var response = request("GET", "/v3/api-docs/swagger-config", null, null);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("/v3/api-docs", json.readTree(response.body()).get("url").asText());
    }

    @Test
    void documentationOffersJwtAuthorizationAndKeepsLoginAndRegistrationPublic() throws Exception {
        var response = request("GET", "/v3/api-docs", null, null);
        assertEquals(200, response.statusCode(), response.body());
        var document = json.readTree(response.body());
        var scheme = document.get("components").get("securitySchemes").get("bearerAuth");
        assertEquals("http", scheme.get("type").asText());
        assertEquals("bearer", scheme.get("scheme").asText());
        assertTrue(document.get("security").get(0).has("bearerAuth"));
        for (String path : new String[]{"/auth/register", "/auth/login"}) {
            var security = document.get("paths").get(path).get("post").get("security");
            assertNotNull(security, "Public endpoint must override the global JWT requirement: " + path);
            assertTrue(security.isArray() && security.isEmpty(), path);
        }
    }

    @Test
    void publicDocumentationDoesNotRemoveAuthenticationFromBusinessEndpoints() throws Exception {
        assertEquals(401, request("GET", "/reports", null, null).statusCode());
        assertEquals(200, request("GET", "/reports", token, null).statusCode());
    }
}
