package sdu.ai.lab.authservice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @JsonIgnore
    private String email;

    @NotBlank(message = "{validation.oldPassword.required}")
    @Size(min = 8, max = 255, message = "{validation.oldPassword.size}")
    private String oldPassword;

    @NotBlank(message = "{validation.newPassword.required}")
    @Size(min = 8, max = 255, message = "{validation.newPassword.size}")
    private String newPassword;
}