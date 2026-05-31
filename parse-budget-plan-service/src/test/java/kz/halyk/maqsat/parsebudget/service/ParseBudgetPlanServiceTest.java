package kz.halyk.maqsat.parsebudget.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.halyk.maqsat.parsebudget.client.BudgetClient;
import kz.halyk.maqsat.parsebudget.dto.ActivePlanView;
import kz.halyk.maqsat.parsebudget.dto.BudgetPlanProposal;
import kz.halyk.maqsat.parsebudget.dto.PlannedCategory;
import kz.halyk.maqsat.parsebudget.dto.ReplanRequest;
import kz.halyk.maqsat.parsebudget.dto.ReplanResponse;
import kz.halyk.maqsat.parsebudget.exception.InvalidPlanException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParseBudgetPlanServiceTest {

    @Mock
    private BudgetClient budgetClient;

    @org.mockito.Spy
    private io.micrometer.core.instrument.MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @InjectMocks
    private ParseBudgetPlanService service;

    @Test
    void happyPath_returnsReplanResponseWithNewPlanId() {
        UUID existingPlanId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        String userId = "user-1";

        ActivePlanView active = new ActivePlanView(existingPlanId, "2026-05", List.of());
        when(budgetClient.getActive(userId)).thenReturn(Optional.of(active));
        when(budgetClient.replan(eq(userId), any())).thenReturn(newPlanId);

        ReplanRequest req = new ReplanRequest(
                userId,
                "2026-05",
                new BudgetPlanProposal(
                        List.of(new PlannedCategory("Продукты", "MANDATORY", new BigDecimal("100000"))),
                        "drift detected"
                ),
                null
        );

        ReplanResponse resp = service.handle(req);

        assertThat(resp.newPlanId()).isEqualTo(newPlanId);
        assertThat(resp.supersededPlanId()).isEqualTo(existingPlanId);
    }

    @Test
    void emptyCategories_throwsInvalidPlanException() {
        ReplanRequest req = new ReplanRequest(
                "user-1",
                "2026-05",
                new BudgetPlanProposal(List.of(), "rationale"),
                null
        );

        assertThatThrownBy(() -> service.handle(req))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("no categories");
    }

    @Test
    void badType_throwsInvalidPlanException() {
        ReplanRequest req = new ReplanRequest(
                "user-1",
                "2026-05",
                new BudgetPlanProposal(
                        List.of(new PlannedCategory("Продукты", "UNKNOWN_TYPE", new BigDecimal("100000"))),
                        "rationale"
                ),
                null
        );

        assertThatThrownBy(() -> service.handle(req))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Invalid type");
    }

    @Test
    void getActiveReturnsEmpty_supersededPlanIdIsNull() {
        UUID newPlanId = UUID.randomUUID();
        String userId = "user-2";

        when(budgetClient.getActive(userId)).thenReturn(Optional.empty());
        when(budgetClient.replan(eq(userId), any())).thenReturn(newPlanId);

        ReplanRequest req = new ReplanRequest(
                userId,
                "2026-05",
                new BudgetPlanProposal(
                        List.of(new PlannedCategory("Коммуналка", "MANDATORY", new BigDecimal("50000"))),
                        "first plan"
                ),
                null
        );

        ReplanResponse resp = service.handle(req);

        assertThat(resp.newPlanId()).isEqualTo(newPlanId);
        assertThat(resp.supersededPlanId()).isNull();
    }
}
