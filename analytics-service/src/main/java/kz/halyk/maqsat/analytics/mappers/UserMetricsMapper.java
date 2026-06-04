package kz.halyk.maqsat.analytics.mappers;

import java.util.List;
import kz.halyk.maqsat.analytics.dto.res.UserMetricsResponse;
import kz.halyk.maqsat.analytics.entities.CategoryStat;
import kz.halyk.maqsat.analytics.entities.RecurringDebit;
import kz.halyk.maqsat.analytics.entities.UserMetrics;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMetricsMapper {

    UserMetricsResponse.CategoryStatView toCategoryStatView(CategoryStat stat);

    UserMetricsResponse.RecurringDebitView toRecurringDebitView(RecurringDebit rec);

    default UserMetricsResponse toResponse(UserMetrics m, List<CategoryStat> cats, List<RecurringDebit> recurring) {
        return new UserMetricsResponse(
                m.getId(), m.getUserId(), m.getPeriod(),
                m.getIncomeEstimate(), m.getTotalSpent(),
                m.getAvgTransaction(), m.getMedianTransaction(),
                m.getVolatility(), m.getSavingsRate(), m.getComputedAt(),
                cats.stream().map(this::toCategoryStatView).toList(),
                recurring.stream().map(this::toRecurringDebitView).toList()
        );
    }
}
