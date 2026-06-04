package kz.halyk.maqsat.budget.mappers;

import kz.halyk.maqsat.budget.dto.res.ActivePlanView;
import kz.halyk.maqsat.budget.dto.res.DashboardResponse;
import kz.halyk.maqsat.budget.entities.BudgetCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(source = "limitAmount", target = "limit")
    @Mapping(target = "type", expression = "java(c.getType().name())")
    ActivePlanView.CategoryLimitView toCategoryLimitView(BudgetCategory c);

    @Mapping(source = "limitAmount", target = "limit")
    @Mapping(source = "spentAmount", target = "spent")
    @Mapping(target = "type", expression = "java(c.getType().name())")
    @Mapping(target = "remaining", expression = "java(c.getLimitAmount().subtract(c.getSpentAmount()))")
    DashboardResponse.CategoryView toCategoryView(BudgetCategory c);
}
