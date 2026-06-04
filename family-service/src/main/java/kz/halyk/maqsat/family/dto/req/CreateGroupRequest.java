package kz.halyk.maqsat.family.dto.req;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(@NotBlank String name) {}
