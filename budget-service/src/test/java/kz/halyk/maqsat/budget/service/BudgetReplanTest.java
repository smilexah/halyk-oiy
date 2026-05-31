package kz.halyk.maqsat.budget.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import kz.halyk.maqsat.budget.domain.BudgetCategory;
import kz.halyk.maqsat.budget.domain.BudgetPlan;
import kz.halyk.maqsat.budget.domain.CategoryType;
import kz.halyk.maqsat.budget.domain.OwnerType;
import kz.halyk.maqsat.budget.dto.ParsedPlanPayload;
import kz.halyk.maqsat.budget.repository.BudgetPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(BudgetService.class)
class BudgetReplanTest {

    @Autowired
    BudgetService svc;

    @Autowired
    BudgetPlanRepository repo;

    @MockBean
    MeterRegistry meterRegistry;

    @Test
    void firstReplan_noExisting_version1() {
        ParsedPlanPayload payload = new ParsedPlanPayload(
                OwnerType.USER, "u-new", "2026-05",
                List.of(new ParsedPlanPayload.PlannedCategory("Продукты", CategoryType.MANDATORY, new BigDecimal("100000"))),
                true, "first replan");

        BudgetPlan p = svc.replan("u-new", payload);

        assertThat(p.getVersion()).isEqualTo(1);
        assertThat(p.isCreatedByAi()).isTrue();
        assertThat(p.getSupersededBy()).isNull();
    }

    @Test
    void replanInsertsNewVersionAndSupersedesOld() {
        // Given an existing plan v1
        BudgetPlan p1 = new BudgetPlan();
        p1.setOwnerId("u1");
        p1.setOwnerType(OwnerType.USER);
        p1.setPeriodStart(LocalDate.of(2026, 5, 1));
        p1.setPeriodEnd(LocalDate.of(2026, 5, 31));
        p1.setCreatedAt(Instant.now());
        BudgetCategory c = new BudgetCategory();
        c.setName("Продукты");
        c.setType(CategoryType.MANDATORY);
        c.setLimitAmount(new BigDecimal("100000"));
        c.setSpentAmount(BigDecimal.ZERO);
        p1.addCategory(c);
        repo.save(p1);

        // When AI re-plans
        ParsedPlanPayload payload = new ParsedPlanPayload(
                OwnerType.USER, "u1", "2026-05",
                List.of(new ParsedPlanPayload.PlannedCategory("Продукты", CategoryType.MANDATORY, new BigDecimal("130000"))),
                true, "drift_detected");
        BudgetPlan p2 = svc.replan("u1", payload);

        // Then
        assertThat(p2.getVersion()).isEqualTo(2);
        assertThat(p2.isCreatedByAi()).isTrue();
        BudgetPlan reloadedOld = repo.findById(p1.getId()).orElseThrow();
        assertThat(reloadedOld.getSupersededBy()).isEqualTo(p2.getId());
    }

    @Test
    void getActivePlan_afterReplan_returnsNewPlan() {
        // Set up existing plan v1
        BudgetPlan p1 = new BudgetPlan();
        p1.setOwnerId("u2");
        p1.setOwnerType(OwnerType.USER);
        p1.setPeriodStart(LocalDate.now().withDayOfMonth(1));
        p1.setPeriodEnd(LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1));
        p1.setCreatedAt(Instant.now());
        BudgetCategory cat = new BudgetCategory();
        cat.setName("Транспорт");
        cat.setType(CategoryType.MANDATORY);
        cat.setLimitAmount(new BigDecimal("50000"));
        cat.setSpentAmount(BigDecimal.ZERO);
        p1.addCategory(cat);
        repo.save(p1);

        // Replan
        String period = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        ParsedPlanPayload payload = new ParsedPlanPayload(
                OwnerType.USER, "u2", period,
                List.of(new ParsedPlanPayload.PlannedCategory("Транспорт", CategoryType.MANDATORY, new BigDecimal("60000"))),
                true, "drift");
        BudgetPlan p2 = svc.replan("u2", payload);

        // getActivePlan should return the new plan (not the superseded one)
        var activeView = svc.getActivePlan("u2");
        assertThat(activeView).isNotNull();
        assertThat(activeView.planId()).isEqualTo(p2.getId());
    }
}
