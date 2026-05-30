package kz.halyk.maqsat.transaction.client;

import kz.halyk.maqsat.transaction.dto.ChildLimitView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Reads a child's daily limit from family-service, propagating the caller's bearer. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FamilyClient {

    private final WebClient familyWebClient;

    /** Returns the child's limit, or null when the user is not a limited child (404). */
    public ChildLimitView getChildLimit(String bearer, String userId) {
        try {
            return familyWebClient.get()
                    .uri("/api/family/members/by-user/{userId}/limit", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                    .retrieve()
                    .bodyToMono(ChildLimitView.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        } catch (RuntimeException e) {
            // Fail open: if family-service is unreachable, do not block the transaction.
            log.warn("Could not read child limit for {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
