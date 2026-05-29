package sdu.ai.lab.authservice.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static sdu.ai.lab.authservice.constants.ValueConstants.*;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {
    public static String extractIdFromToken(JwtAuthenticationToken token) {
        String userId = token.getToken().getClaim(USER_ID_CLAIM);
        if (userId == null) {
            userId = token.getToken().getSubject();
        }
        return userId;
    }

    public static String extractNameFromToken(JwtAuthenticationToken token) {
        return token.getToken().getClaim(USER_NAME_CLAIM);
    }

    public static List<String> getAuthorities(Collection<GrantedAuthority> authorities) {
        return authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public static LocalDateTime getLocalDateTimeFromTimestamp(Long timestamp) {
        if (timestamp == null) return null;
        return Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).toLocalDateTime();
    }

    public static long getTimeStampFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return 0;
        return Timestamp.valueOf(localDateTime).getTime();
    }
}