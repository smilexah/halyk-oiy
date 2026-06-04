package kz.halyk.maqsat.auth.services.impl;

import java.security.SecureRandom;
import kz.halyk.maqsat.auth.client.FamilyClient;
import kz.halyk.maqsat.auth.client.KeycloakAdminClient;
import kz.halyk.maqsat.auth.dto.req.InviteRequest;
import kz.halyk.maqsat.auth.dto.res.InviteResponse;
import kz.halyk.maqsat.auth.services.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final KeycloakAdminClient keycloakAdminClient;
    private final FamilyClient familyClient;

    @Override
    public InviteResponse invite(String initiatorBearer, InviteRequest request) {
        String temporaryPassword = generateTemporaryPassword();
        String adminToken = keycloakAdminClient.obtainAdminToken();
        String userId = keycloakAdminClient.createInvitedUser(adminToken, request, temporaryPassword);
        familyClient.addMember(initiatorBearer, request.groupId(), userId, request.role().name(), request.dailyLimit());
        log.info("Invited {} as {} into group {}", request.username(), request.role(), request.groupId());
        return new InviteResponse(
                userId, request.username(), temporaryPassword, request.role().name(), request.groupId(),
                "User created with a temporary password and must change it on first login (UPDATE_PASSWORD).");
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
