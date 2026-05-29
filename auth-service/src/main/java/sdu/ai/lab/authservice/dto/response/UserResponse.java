package sdu.ai.lab.authservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Информация о пользователе")
public class UserResponse {
    @Schema(description = "ID пользователя", example = "1")
    private Long id;

    @Schema(description = "Email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Номер телефона", example = "+77001234567")
    private String phoneNumber;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    private String lastName;

    @Schema(description = "Активен ли пользователь (enabled в Keycloak)", example = "true")
    private Boolean isActive;

    @Schema(description = "Роль пользователя", example = "ADMIN")
    private String role;
}
