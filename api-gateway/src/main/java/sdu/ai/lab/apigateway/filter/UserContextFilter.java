package sdu.ai.lab.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

// После того как Spring Security провалидировал токен Keycloak,
// этот фильтр извлекает claims и передаёт их downstream-сервисам как заголовки.
// Downstream-сервисы читают X-User-Id / X-User-Roles вместо того чтобы парсить токен сами.
@Slf4j
@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    var jwt = auth.getToken();
                    var roles = extractRoles(jwt.getClaims());

                    var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", jwt.getSubject())
                            .header("X-User-Email", jwt.getClaimAsString("email"))
                            .header("X-User-Roles", roles)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange)); // публичные пути без токена
    }

    // Keycloak кладёт роли realm-уровня в realm_access.roles
    @SuppressWarnings("unchecked")
    private String extractRoles(Map<String, Object> claims) {
        try {
            var realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess == null) return "";
            var roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return "";
            return String.join(",", roles);
        } catch (ClassCastException e) {
            return "";
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}