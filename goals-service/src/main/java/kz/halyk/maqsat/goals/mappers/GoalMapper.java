package kz.halyk.maqsat.goals.mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import kz.halyk.maqsat.goals.dto.res.GoalResponse;
import kz.halyk.maqsat.goals.entities.Goal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GoalMapper {

    @Mapping(target = "progressPercent", expression = "java(computeProgress(g))")
    @Mapping(source = "virtualAccount.balance", target = "virtualAccountBalance")
    GoalResponse toResponse(Goal g);

    default BigDecimal computeProgress(Goal g) {
        if (g.getTargetAmount() == null || g.getTargetAmount().signum() <= 0) return BigDecimal.ZERO;
        return g.getAllocatedAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP);
    }
}
