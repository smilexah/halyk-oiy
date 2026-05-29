package sdu.ai.lab.authservice.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfigProperties {
    private String realm;
    private String clientId;
    private String clientSecret;
    private String url;
    private String adminUsername;
    private String adminPassword;
    private String adminRealm = "master";
    private String adminClientId = "admin-cli";
    private boolean sendEmail;
}