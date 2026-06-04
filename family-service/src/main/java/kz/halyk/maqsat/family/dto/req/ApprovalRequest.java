package kz.halyk.maqsat.family.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ApprovalRequest(
        @NotBlank String childUserId,
        @NotNull @Positive BigDecimal approvedAmount
) {}
