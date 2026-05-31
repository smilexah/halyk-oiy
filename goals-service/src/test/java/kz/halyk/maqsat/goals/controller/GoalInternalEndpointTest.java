package kz.halyk.maqsat.goals.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.domain.Goal;
import kz.halyk.maqsat.goals.domain.VirtualAccount;
import kz.halyk.maqsat.goals.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoalInternalEndpointTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    GoalService service;

    @Test
    void returnsGoalsForUser() throws Exception {
        VirtualAccount account = new VirtualAccount();
        account.setOwnerId("u1");
        account.setBalance(BigDecimal.valueOf(100000));

        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setName("Trip");
        goal.setCategory("travel");
        goal.setTargetAmount(BigDecimal.valueOf(500000));
        goal.setAllocatedAmount(BigDecimal.valueOf(100000));
        goal.setCreatedAt(Instant.now());
        goal.setVirtualAccount(account);

        when(service.internalListForUser("u1")).thenReturn(List.of(goal));

        mvc.perform(get("/api/goals/internal/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trip"));
    }
}
