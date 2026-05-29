package sdu.ai.lab.authservice.security.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeycloakBaseUser {
    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 255, message = "{validation.firstName.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 255, message = "{validation.lastName.size}")
    private String lastName;

    @NotBlank(message = "{validation.username.required}")
    @Size(max = 255, message = "{validation.username.size}")
    private String username;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 255, message = "{validation.password.size}")
    private String password;

    @NotBlank(message = "{validation.phoneNumber.required}")
    @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "{validation.phoneNumber.pattern}")
    private String phoneNumber;

    @JsonIgnore
    private String keycloakId;
}