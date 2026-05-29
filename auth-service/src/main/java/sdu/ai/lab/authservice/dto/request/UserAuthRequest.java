package sdu.ai.lab.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAuthRequest(
        @NotBlank(message = "{validation.auth.email.required}")
        @Email(message = "{validation.auth.email.invalid}")
        @Size(max = 255, message = "{validation.auth.email.size}")
        String email,

        @NotBlank(message = "{validation.auth.password.required}")
        @Size(min = 5, max = 255, message = "{validation.auth.password.size}")
        String password
) {
}