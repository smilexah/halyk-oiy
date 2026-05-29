package sdu.ai.lab.authservice.mappers;

import sdu.ai.lab.authservice.dto.response.UserResponse;
import sdu.ai.lab.authservice.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "role", ignore = true)
    UserResponse toDto(User user);

    default UserResponse toDtoWithStatus(User user, Boolean isActive) {
        UserResponse response = toDto(user);
        response.setIsActive(isActive);
        return response;
    }

    default UserResponse toDtoWithStatusAndRole(User user, Boolean isActive, String role) {
        UserResponse response = toDto(user);
        response.setIsActive(isActive);
        response.setRole(role);
        return response;
    }
}
