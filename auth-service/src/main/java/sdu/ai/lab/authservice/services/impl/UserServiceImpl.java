package sdu.ai.lab.authservice.services.impl;

import jakarta.persistence.EntityManager;
import sdu.ai.lab.authservice.dto.response.UserResponse;
import sdu.ai.lab.authservice.entities.User;
import sdu.ai.lab.authservice.exceptions.DbObjectNotFoundException;
import sdu.ai.lab.authservice.mappers.UserMapper;
import sdu.ai.lab.authservice.repositories.UserRepository;
import sdu.ai.lab.authservice.security.keycloak.KeycloakBaseUser;
import sdu.ai.lab.authservice.security.keycloak.KeycloakRole;
import sdu.ai.lab.authservice.services.KeycloakService;
import sdu.ai.lab.authservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final EntityManager entityManager;
    private final MessageSource messageSource;
    private final UserMapper userMapper;

    @Value("${keycloak.admin-username}")
    private String keycloakUsername;

    @Value("${keycloak.admin-password}")
    private String keycloakPassword;

    @Override
    public User getUserByKeycloakId(String keycloakId) {
        log.info("Retrieving user with keycloakId: {}", keycloakId);
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new DbObjectNotFoundException(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), messageSource.getMessage("error.user.notFound", null, LocaleContextHolder.getLocale())));
    }

    @Override
    @Transactional
    public void syncUsersBetweenDBAndKeycloak() {
        var usersFromDB = userRepository.findAll();

        var usersFromKeycloak = keycloakService.getAllUsers();

        log.info("usersFromKeycloak(with admin account and testers): {}, usersFromDB: {}", usersFromKeycloak.size(), usersFromDB.size());

        AtomicReference<String> testerUserId = new AtomicReference<>("");


        var usersToDeleteFromKeycloak = usersFromKeycloak.stream()
                .filter(user -> {
                            if (user.getEmail() != null && user.getEmail().equals(keycloakUsername)) {
                                testerUserId.set(user.getId());
                                return false;
                            }

                            return usersFromDB
                                    .stream()
                                    .noneMatch(user1 -> user1.getKeycloakId()
                                            .equals(user.getId()))
                                    &&
                                    !user.getUsername().equals(keycloakUsername);
                        }
                )
                .toList();

        for (var user : usersToDeleteFromKeycloak) {
            keycloakService.deleteUserById(user.getId());
        }

        AtomicBoolean testUserExists = new AtomicBoolean(false);

        List<User> usersToDeleteFromDB = usersFromDB
                .stream()
                .filter(user -> {
                    if (user.getKeycloakId().equals(testerUserId.get())) {
                        testUserExists.set(true);
                    }
                    return usersFromKeycloak
                            .stream()
                            .noneMatch(userRepresentation -> userRepresentation
                                    .getId()
                                    .equals(user.getKeycloakId()));
                })
                .toList();
        log.info("usersToDeleteFromKeycloak: {}, usersToDeleteFromDB: {}", usersToDeleteFromKeycloak.size(), usersToDeleteFromDB.size());

        if (!usersToDeleteFromDB.isEmpty()) {
            userRepository.deleteAll(usersToDeleteFromDB);
            entityManager.flush();
            entityManager.clear();
        }


        log.info("Test user exists in db: {}, test user exists in keycloak: {}", testUserExists.get(), !testerUserId.get().isEmpty());

        log.info("Tester id: {}", testerUserId.get());

        if (!testUserExists.get()) {
            if (testerUserId.get().isEmpty()) {
                var keycloakUser = new KeycloakBaseUser();
                keycloakUser.setEmail(keycloakUsername);
                keycloakUser.setPassword(keycloakPassword);
                keycloakUser.setFirstName("Akhan");
                keycloakUser.setLastName("Dulatbay");
                var keycloakTester = keycloakService.createUserByRole(keycloakUser,
                        KeycloakRole.ADMIN
                );
                testerUserId.set(keycloakTester.getId());
            }

            var user = new User();
            user.setKeycloakId(testerUserId.get());
            user.setPhoneNumber("tester");
            user.setFirstName(keycloakUsername);
            user.setLastName("tester");

            userRepository.save(user);
            log.info("New tester created");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String keycloakId) {
        log.info("Fetching current user info for keycloakId: {}", keycloakId);

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new DbObjectNotFoundException(HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found with Keycloak ID: " + keycloakId));

        // Получаем статус активности и роль из Keycloak
        Boolean isActive = false;
        String role = null;
        try {
            UserRepresentation keycloakUser = keycloakService.getUserById(keycloakId).toRepresentation();
            isActive = keycloakUser.isEnabled();
            role = keycloakService.getUserRole(keycloakId);
        } catch (Exception e) {
            log.warn("Failed to fetch user status or role from Keycloak for user: {}", keycloakId, e);
        }

        log.info("Successfully fetched user info: {} {} with role: {}", user.getFirstName(), user.getLastName(), role);
        return userMapper.toDtoWithStatusAndRole(user, isActive, role);
    }
}
