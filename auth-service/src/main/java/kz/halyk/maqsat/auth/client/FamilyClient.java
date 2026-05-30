package kz.halyk.maqsat.auth.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Calls family-service (through Eureka), propagating the inviting adult's bearer token. */
@Component
@RequiredArgsConstructor
public class FamilyClient {

    private final WebClient familyWebClient;

    public void addMember(String initiatorBearer, UUID groupId, String userId, String role, BigDecimal dailyLimit) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("role", role);
        if (dailyLimit != null) {
            body.put("dailyLimit", dailyLimit);
        }

        familyWebClient.post()
                .uri("/api/family/groups/{id}/members", groupId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + initiatorBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
