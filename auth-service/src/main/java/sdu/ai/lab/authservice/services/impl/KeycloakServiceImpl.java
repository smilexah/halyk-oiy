package sdu.ai.lab.authservice.services.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import sdu.ai.lab.authservice.config.properties.KeycloakConfigProperties;
import sdu.ai.lab.authservice.dto.request.UpdatePasswordRequest;
import sdu.ai.lab.authservice.dto.response.AuthResponse;
import sdu.ai.lab.authservice.dto.response.KeycloakTokenResponse;
import sdu.ai.lab.authservice.entities.User;
import sdu.ai.lab.authservice.entities.enums.TokenType;
import sdu.ai.lab.authservice.exceptions.UnauthorizedException;
import sdu.ai.lab.authservice.repositories.UserRepository;
import sdu.ai.lab.authservice.security.keycloak.KeycloakBaseUser;
import sdu.ai.lab.authservice.security.keycloak.KeycloakRole;
import sdu.ai.lab.authservice.services.KeycloakService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {
    private final RestTemplate restTemplate;
    private final KeycloakConfigProperties keycloakConfigProperties;
    private final MessageSource messageSource;
    private final UserRepository userRepository;

    private Keycloak adminKeycloak;

    @PostConstruct
    private void init() {
        adminKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfigProperties.getUrl())
                .realm(keycloakConfigProperties.getAdminRealm())
                .clientId(keycloakConfigProperties.getAdminClientId())
                .grantType(OAuth2Constants.PASSWORD)
                .username(keycloakConfigProperties.getAdminUsername())
                .password(keycloakConfigProperties.getAdminPassword())
                .resteasyClient(new ResteasyClientBuilderImpl()
                        .connectionPoolSize(10)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public UserRepresentation createUserByRole(KeycloakBaseUser baseUser, KeycloakRole keycloakRole) {
        UserRepresentation userRepresentation = setupUserRepresentation(baseUser);
        String userId = null;
        try (Response response = getUsersResource().create(userRepresentation)) {
            handleUnsuccessfulResponse(response);
            userId = CreatedResponseUtil.getCreatedId(response);
            UserResource userResource = setupUserResource(baseUser, keycloakRole, userId);

            if (keycloakConfigProperties.isSendEmail()) {
                try {
                    sendEmail(userId);
                } catch (Exception e) {
                    log.error("Exception: ", e);
                    throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.invalidEmail", null, LocaleContextHolder.getLocale()));
                }
            }

            User user = new User();
            user.setKeycloakId(userId);
            user.setFirstName(baseUser.getFirstName());
            user.setLastName(baseUser.getLastName());
            user.setEmail(baseUser.getEmail());
            user.setPhoneNumber(baseUser.getPhoneNumber());

            try {
                userRepository.save(user);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.error("Data integrity violation while saving user: {}", user, e);
                handleExceptionAfterUserIdCreated(userId);

                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    String lowerCaseMessage = errorMessage.toLowerCase();
                    if (lowerCaseMessage.contains("username") || lowerCaseMessage.contains("user_name")) {
                        throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.usernameExists", null, LocaleContextHolder.getLocale()));
                    } else if (lowerCaseMessage.contains("email")) {
                        throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.emailExists", null, LocaleContextHolder.getLocale()));
                    }
                }
                throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.userAlreadyExists", null, LocaleContextHolder.getLocale()));
            } catch (Exception e) {
                log.error("Unexpected error while saving user in the database: {}", user, e);
                handleExceptionAfterUserIdCreated(userId);
                throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.failedToSaveUser", null, LocaleContextHolder.getLocale()), e);
            }

            return userResource.toRepresentation();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            handleExceptionAfterUserIdCreated(userId);
            throw e;
        }
    }

    private UserRepresentation setupUserRepresentation(KeycloakBaseUser request) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setFirstName(request.getFirstName());
        userRepresentation.setLastName(request.getLastName());
        userRepresentation.setEmail(request.getEmail());
        userRepresentation.setUsername(request.getEmail());
        userRepresentation.setEmailVerified(true);
        return userRepresentation;
    }

    private void handleUnsuccessfulResponse(Response response) {
        if (response.getStatus() != 201) {
            log.error("Keycloak response status: {}", response.getStatus());

            String errorMessage = "Unknown error";
            try {
                if (response.hasEntity()) {
                    var entity = response.readEntity(String.class);
                    log.error("Keycloak error response body: {}", entity);
                    errorMessage = entity;
                }
            } catch (Exception e) {
                log.error("Failed to read error response entity", e);
            }

            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new IllegalArgumentException("User already exists: " + errorMessage);
            } else if (response.getStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new IllegalArgumentException("Insufficient permissions to create user. Please check Keycloak admin credentials and roles. Details: " + errorMessage);
            } else if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                throw new IllegalArgumentException("Unauthorized to create user. Please check Keycloak admin credentials. Details: " + errorMessage);
            } else {
                throw new InternalServerErrorException("Keycloak error (status " + response.getStatus() + "): " + errorMessage);
            }
        }
    }

//    private void handleUnsuccessfulResponse(Response response) {
//        if (response.getStatus() != 201) {
//            log.error("Keycloak response status: {}", response.getStatus());
//
//            String errorMessage = "Unknown error";
//            try {
//                if (response.hasEntity()) {
//                    var entity = response.readEntity(String.class);
//                    log.error("Keycloak error response body: {}", entity);
//                    errorMessage = entity;
//                }
//            } catch (Exception e) {
//                log.error("Failed to read error response entity", e);
//            }
//
//            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
//                throw new IllegalArgumentException("User already exists: " + errorMessage);
//            } else if (response.getStatus() == HttpStatus.FORBIDDEN.value()) {
//                throw new IllegalArgumentException("Insufficient permissions to create user. Please check Keycloak admin credentials and roles. Details: " + errorMessage);
//            } else if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
//                throw new IllegalArgumentException("Unauthorized to create user. Please check Keycloak admin credentials. Details: " + errorMessage);
//            } else {
//                throw new InternalServerErrorException("Keycloak error (status " + response.getStatus() + "): " + errorMessage);
//            }
//        }
//    }

    private UserResource setupUserResource(KeycloakBaseUser keycloakBaseUser, KeycloakRole keycloakRole, String userId) {
        UserResource userResource = getUsersResource().get(userId);
        userResource.resetPassword(getPasswordCredential(keycloakBaseUser.getPassword()));
        userResource.roles()
                .clientLevel(getClient().getId())
                .add(Collections.singletonList(getClientRole(getClient(), keycloakRole)));
        return userResource;
    }

    private void handleExceptionAfterUserIdCreated(String userId) {
        if (userId != null) getUsersResource().delete(userId);
    }

    private CredentialRepresentation getPasswordCredential(String password) {
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);
        return passwordCred;
    }

    private RoleRepresentation getClientRole(ClientRepresentation client, KeycloakRole keycloakRole) {
        return getRealmResource().clients().get(client.getId())
                .roles().get(keycloakRole.getValue()).toRepresentation();
    }

    private Keycloak getAdminKeycloak() {
        return adminKeycloak;
    }

    private RealmResource getRealmResource() {
        return getAdminKeycloak().realm(this.keycloakConfigProperties.getRealm());
    }

    private UsersResource getUsersResource() {
        return getRealmResource().users();
    }

    private ClientRepresentation getClient() {
        return getRealmResource().clients()
                .findByClientId(this.keycloakConfigProperties.getClientId()).getFirst();
    }

    private void sendEmail(String userId) {
        getUsersResource().get(userId).sendVerifyEmail();
    }

    private Keycloak getCurrentKeycloak(String username, String password) {
        return KeycloakBuilder.builder()
                .serverUrl(this.keycloakConfigProperties.getUrl())
                .realm(this.keycloakConfigProperties.getRealm())
                .clientId(this.keycloakConfigProperties.getClientId())
                .clientSecret(this.keycloakConfigProperties.getClientSecret())
                .grantType(OAuth2Constants.PASSWORD)
                .username(username)
                .password(password)
                .resteasyClient(new ResteasyClientBuilderImpl()
                        .connectionPoolSize(10)
                        .build())
                .build();
    }

    @Override
    public AuthResponse getAuthResponse(String username, String password, HttpServletResponse response1) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException(messageSource.getMessage("error.auth.usernameOrPasswordEmpty", null, LocaleContextHolder.getLocale()));
        }

        String tokenUrl = buildTokenEndpoint("token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", this.keycloakConfigProperties.getClientId());
        form.add("client_secret", this.keycloakConfigProperties.getClientSecret());
        form.add("username", username);
        form.add("password", password);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            var response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var body = response.getBody();
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                Integer expiresIn = (Integer) body.get("expires_in");
                Integer refreshExpiresIn = (Integer) body.get("refresh_expires_in");
                String tokenType = (String) body.get("token_type");

                Cookie refreshTokenCookie = addCookieWithSameSite(TokenType.refreshToken.name(), refreshToken, refreshExpiresIn, true);
                response1.addCookie(refreshTokenCookie);

                String role = extractRoleFromToken(accessToken);
                Cookie roleCookie = addCookieWithSameSite("USER_ROLE", role, refreshExpiresIn, false);
                response1.addCookie(roleCookie);

                return buildAuthResponseDto(accessToken, refreshToken, Long.valueOf(expiresIn), tokenType);
            } else {
                throw new RuntimeException(messageSource.getMessage("error.auth.failedToGetAuthResponse", 
                        new Object[]{response.getStatusCode()}, LocaleContextHolder.getLocale()));
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Authentication failed for user '{}': Invalid credentials", username);
            throw new UnauthorizedException(messageSource.getMessage("error.auth.invalidCredentials", null, LocaleContextHolder.getLocale()));
        } catch (HttpClientErrorException e) {
            log.error("HTTP error from Keycloak: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UnauthorizedException(messageSource.getMessage("error.auth.invalidCredentials", null, LocaleContextHolder.getLocale()));
            }
            throw new UnauthorizedException(messageSource.getMessage("error.auth.invalidCredentials", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            log.error("Unexpected error during authentication: ", e);
            throw new UnauthorizedException(messageSource.getMessage("error.auth.invalidCredentials", null, LocaleContextHolder.getLocale()));
        }
    }

    @Override
    public void deleteUserById(String userId) {
        log.info("Deleted user with id {}", userId);
        getUserById(userId).remove();
    }

    @Override
    public void logout(HttpServletRequest request1, HttpServletResponse response1) {
        String logoutUrl = buildTokenEndpoint("logout");

        Cookie refreshTokenCookie = extractRefreshTokenCookie(request1);
        String refreshToken = refreshTokenCookie.getValue();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(messageSource.getMessage("error.auth.refreshTokenInvalid", null, LocaleContextHolder.getLocale()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", this.keycloakConfigProperties.getClientId());
        form.add("client_secret", this.keycloakConfigProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Cookie deleteRefreshTokenCookie = new Cookie(TokenType.refreshToken.name(), null);
                deleteRefreshTokenCookie.setMaxAge(0);
                deleteRefreshTokenCookie.setPath("/");
                response1.addCookie(deleteRefreshTokenCookie);

                // Удаляем role cookie
                Cookie deleteRoleCookie = new Cookie("USER_ROLE", null);
                deleteRoleCookie.setMaxAge(0);
                deleteRoleCookie.setPath("/");
                response1.addCookie(deleteRoleCookie);

                log.info("User logged out successfully");
            } else {
                log.error("Failed to logout user: {}", response.getStatusCode());
                throw new RuntimeException(messageSource.getMessage("error.auth.failedToLogout", 
                        new Object[]{response.getStatusCode()}, LocaleContextHolder.getLocale()));
            }
        } catch (Exception e) {
            log.error("Error during logout: ", e);
            throw new RuntimeException(messageSource.getMessage("error.auth.logoutError", 
                    new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()));
        }
    }

    @Override
    public List<UserRepresentation> getAllUsers() {
        return getUsersResource().list();
    }

    @Override
    public List<UserRepresentation> getAllUsersByRole(KeycloakRole keycloakRole) {
        return getRealmResource()
                .clients()
                .get(getClient().getId())
                .roles().get(keycloakRole.getValue())
                .getUserMembers();
    }

    @Override
    public UserResource getUserById(String id) {
        return getUsersResource().get(id);
    }

    @Override
    public UserResource updateUser(KeycloakBaseUser keycloakBaseUser) {
        var usersResource = getUsersResource();
        List<UserRepresentation> users = usersResource.search(keycloakBaseUser.getEmail(), 0, 1);

        if (users.isEmpty()) {
            throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.userNotFoundByEmail", null, LocaleContextHolder.getLocale()));
        }
        UserRepresentation userToUpdate = users.getFirst();
        userToUpdate.setFirstName(keycloakBaseUser.getFirstName());
        userToUpdate.setLastName(keycloakBaseUser.getLastName());
        userToUpdate.setEmail(keycloakBaseUser.getEmail());

        UserResource userResource = usersResource.get(userToUpdate.getId());
        userResource.update(userToUpdate);

        log.info("Update user: {}", userResource);

        return userResource;
    }

    @Override
    public void updatePassword(String keycloakId, UpdatePasswordRequest updatePassword) {
        try {
            var keycloak = getCurrentKeycloak(updatePassword.getEmail(), updatePassword.getOldPassword());
            keycloak.tokenManager().getAccessTokenString();

            UserResource userResource = getUsersResource().get(keycloakId);

            CredentialRepresentation newPassword = new CredentialRepresentation();
            newPassword.setType(CredentialRepresentation.PASSWORD);
            newPassword.setValue(updatePassword.getNewPassword());
            newPassword.setTemporary(false);


            userResource.resetPassword(newPassword);
        } catch (jakarta.ws.rs.NotAuthorizedException e) {
            log.error("Old password is incorrect for user with ID: {}", keycloakId);
            throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.incorrectOldPassword", null, LocaleContextHolder.getLocale()));
        }
    }

    @Override
    public void enableUser(String keycloakId, boolean enabled) {
        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setEnabled(enabled);
            userResource.update(userRepresentation);

            log.info("User {} has been {}", keycloakId, enabled ? "activated" : "deactivated");
        } catch (Exception e) {
            log.error("Failed to {} user with ID: {}", enabled ? "activate" : "deactivate", keycloakId, e);
            throw new IllegalArgumentException("Не удалось " + (enabled ? "активировать" : "деактивировать") + " пользователя: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AuthResponse registerUser(KeycloakBaseUser req, KeycloakRole keycloakRole, HttpServletResponse response) {
        validate(req);
        this.createUserByRole(req, keycloakRole);
        return this.loginAsNewUser(req.getEmail(), req.getPassword(), response);
    }

    private void validate(KeycloakBaseUser r) {
        if (r == null
                || isBlank(r.getEmail())
                || isBlank(r.getLastName())
                || isBlank(r.getPassword())
                || isBlank(r.getFirstName())) {
            throw new IllegalArgumentException(messageSource.getMessage("error.keycloak.invalidRegistrationRequest", null, LocaleContextHolder.getLocale()));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private AuthResponse loginAsNewUser(String userEmail, String userPassword, HttpServletResponse response) {
        try (var kc = this.getCurrentKeycloak(userEmail, userPassword)) {

            var token = kc.tokenManager().getAccessToken();

            if (token.getRefreshToken() != null) {
                Cookie refreshTokenCookie = addCookieWithSameSite(TokenType.refreshToken.name(), token.getRefreshToken(),
                        (int) token.getRefreshExpiresIn(), true);

                response.addCookie(refreshTokenCookie);
            }

            String role = extractRoleFromToken(token.getToken());
            if (role != null) {
                Cookie roleCookie = addCookieWithSameSite("USER_ROLE", role, (int) token.getRefreshExpiresIn(), false);
                response.addCookie(roleCookie);
            }

            return AuthResponse.builder()
                    .accessToken(token.getToken())
                    .expiresIn(token.getExpiresIn())
                    .tokenType(token.getTokenType())
                    .build();
        }
    }

    @Override
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = extractRefreshTokenCookie(request);
        String refreshToken = refreshTokenCookie.getValue();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(messageSource.getMessage("error.auth.refreshTokenRequired", null, LocaleContextHolder.getLocale()));
        }

        String url = buildTokenEndpoint("token");

        RestTemplate rt = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> req = getMultiValueMapHttpEntity(refreshToken, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> resp = rt.postForEntity(
                    url, req, KeycloakTokenResponse.class
            );

            KeycloakTokenResponse body = resp.getBody();
            if (resp.getStatusCode().is2xxSuccessful() && body != null) {
                if (body.refreshToken() != null) {
                    Integer refreshExpiresIn = body.refreshExpiresIn() != null ? body.refreshExpiresIn().intValue() : null;
                    Cookie newRefreshTokenCookie = addCookieWithSameSite(TokenType.refreshToken.name(), body.refreshToken(), refreshExpiresIn, true);
                    response.addCookie(newRefreshTokenCookie);
                }

                String role = extractRoleFromToken(body.accessToken());
                if (role != null) {
                    Cookie roleCookie = addCookieWithSameSite("USER_ROLE", role, null, false);
                    response.addCookie(roleCookie);
                }

                return AuthResponse.builder()
                        .accessToken(body.accessToken())
                        .expiresIn(body.expiresIn())
                        .tokenType(body.tokenType())
                        .build();
            }
            throw new RuntimeException(messageSource.getMessage("error.auth.unexpectedRefreshResponse", 
                    new Object[]{resp.getStatusCode()}, LocaleContextHolder.getLocale()));

        } catch (HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString();
            log.warn("Token refresh failed with status {}: {}", e.getStatusCode(), msg);
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new UnauthorizedException(messageSource.getMessage("error.auth.refreshFailed.invalidGrant", null, LocaleContextHolder.getLocale()));
            }
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UnauthorizedException(messageSource.getMessage("error.auth.refreshFailed.invalidClient", null, LocaleContextHolder.getLocale()));
            }
            throw new UnauthorizedException(messageSource.getMessage("error.auth.refreshFailed", 
                    new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new RuntimeException(messageSource.getMessage("error.auth.refreshFailed", 
                    new Object[]{e.getMessage()}, LocaleContextHolder.getLocale()), e);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getMultiValueMapHttpEntity(String refreshToken, HttpHeaders headers) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakConfigProperties.getClientId());
        if (keycloakConfigProperties.getClientSecret() != null && !keycloakConfigProperties.getClientSecret().isBlank()) {
            form.add("client_secret", keycloakConfigProperties.getClientSecret());
        }
        form.add("refresh_token", refreshToken);

        return new HttpEntity<>(form, headers);
    }

    private String buildTokenEndpoint(String endpointType) {
        String base = keycloakConfigProperties.getUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/realms/" + keycloakConfigProperties.getRealm() + "/protocol/openid-connect/" + endpointType;
    }

    private String extractRoleFromToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(payload);

            com.fasterxml.jackson.databind.JsonNode resourceAccess = jsonNode.get("resource_access");
            if (resourceAccess != null && resourceAccess.has(keycloakConfigProperties.getClientId())) {
                com.fasterxml.jackson.databind.JsonNode clientNode = resourceAccess.get(keycloakConfigProperties.getClientId());
                com.fasterxml.jackson.databind.JsonNode rolesNode = clientNode.get("roles");

                if (rolesNode != null && rolesNode.isArray() && !rolesNode.isEmpty()) {
                    return rolesNode.get(0).asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
        }
        return null;
    }

    private Cookie addCookieWithSameSite(String name, String value, Integer maxAge, boolean httpOnly) {
        int actualMaxAge = (maxAge != null) ? maxAge : 60 * 60 * 24 * 7; // 7 дней по умолчанию

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(actualMaxAge);
        cookie.setAttribute("SameSite", "None");
        
        return cookie;
    }

    private Cookie extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.auth.refreshTokenMissing", null, LocaleContextHolder.getLocale()));
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(TokenType.refreshToken.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(messageSource.getMessage("error.auth.refreshTokenMissing", null, LocaleContextHolder.getLocale())));
    }

    private AuthResponse buildAuthResponseDto(String accessToken, String refreshToken, Long expiresIn, String tokenType) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .tokenType(tokenType)
                .build();
    }

    @Override
    public String getUserRole(String keycloakId) {
        try {
            UserResource userResource = getUsersResource().get(keycloakId);
            var clientRoles = userResource.roles()
                    .clientLevel(getClient().getId())
                    .listAll();

            if (clientRoles != null && !clientRoles.isEmpty()) {
                return clientRoles.getFirst().getName();
            }

            log.warn("No role found for user with keycloakId: {}", keycloakId);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch role for user with keycloakId: {}", keycloakId, e);
            return null;
        }
    }
}
