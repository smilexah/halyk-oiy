package sdu.ai.lab.authservice.services;

import jakarta.transaction.Transactional;
import sdu.ai.lab.authservice.dto.response.UserResponse;
import sdu.ai.lab.authservice.entities.User;

public interface UserService {
    User getUserByKeycloakId(String keycloakId);

    @Transactional
    void syncUsersBetweenDBAndKeycloak();

    UserResponse getCurrentUser(String keycloakId);
}