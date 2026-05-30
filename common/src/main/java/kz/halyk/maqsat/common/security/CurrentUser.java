package kz.halyk.maqsat.common.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated caller, resolved from the JWT {@code sub} claim.
 * Keycloak uses a UUID as the subject, which is our cross-service user id.
 */
public record CurrentUser(String userId) {

    /** Resolve the current user from the servlet SecurityContext. */
    public static CurrentUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No authenticated principal in the SecurityContext");
        }
        return new CurrentUser(auth.getName());
    }

    /** Convenience for owner_id columns typed as uuid. */
    public UUID asUuid() {
        return UUID.fromString(userId);
    }
}