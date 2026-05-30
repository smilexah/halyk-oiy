package kz.halyk.maqsat.family.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(
        @NotBlank String name
) {
}
