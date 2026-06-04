package kz.halyk.maqsat.auth.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import kz.halyk.maqsat.auth.config.KeycloakAdminProperties;
import kz.halyk.maqsat.auth.dto.req.InviteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Talks to the Keycloak Admin REST API using the maqsat-admin service account
 * (client_credentials grant). Creates an invited user with a temporary password
 * and a forced UPDATE_PASSWORD action — no credentials are ever shared.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClient {

    private final WebClient keycloakWebClient;
    private final KeycloakAdminProperties properties;

    /** Obtain an admin access token via the service account. */
    public String obtainAdminToken() {
        Map<String, Object> response = keycloakWebClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", properties.clientId())
                        .with("client_secret", properties.clientSecret()))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Could not obtain Keycloak admin token");
        }
        return (String) response.get("access_token");
    }

    /** Create a user with a temporary password + UPDATE_PASSWORD. Returns the Keycloak user id (= sub). */
    public String createInvitedUser(String adminToken, InviteRequest request, String temporaryPassword) {
        Map<String, Object> payload = Map.of(
                "username", request.username(),
                "email", request.email() != null ? request.email() : request.username() + "@maqsat.kz",
                "enabled", true,
                "emailVerified", false,
                "firstName", request.firstName() != null ? request.firstName() : request.username(),
                "lastName", request.lastName() != null ? request.lastName() : "Maqsat",
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", temporaryPassword,
                        "temporary", true)),
                "requiredActions", List.of("UPDATE_PASSWORD"),
                "realmRoles", List.of("CUSTOMER"));

        ResponseEntity<Void> response = keycloakWebClient.post()
                .uri("/admin/realms/{realm}/users", properties.realm())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();

        URI location = response != null ? response.getHeaders().getLocation() : null;
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location for the created user");
        }
        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        log.info("Created Keycloak user {} -> {}", request.username(), userId);
        return userId;
    }
}
