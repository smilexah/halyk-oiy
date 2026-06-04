package kz.halyk.maqsat.goals.controllers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.goals.dto.res.GoalResponse;
import kz.halyk.maqsat.goals.entities.Goal;
import kz.halyk.maqsat.goals.entities.VirtualAccount;
import kz.halyk.maqsat.goals.mappers.GoalMapper;
import kz.halyk.maqsat.goals.services.GoalService;
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

    @MockBean
    GoalMapper goalMapper;

    @Test
    void returnsGoalsForUser() throws Exception {
        VirtualAccount account = new VirtualAccount();
        account.setOwnerId("u1");
        account.setBalance(BigDecimal.valueOf(100000));

        Goal goal = new Goal();
        UUID id = UUID.randomUUID();
        goal.setId(id);
        goal.setName("Trip");
        goal.setCategory("travel");
        goal.setTargetAmount(BigDecimal.valueOf(500000));
        goal.setAllocatedAmount(BigDecimal.valueOf(100000));
        goal.setCreatedAt(Instant.now());
        goal.setVirtualAccount(account);

        GoalResponse response = new GoalResponse(id, "Trip", null, "travel",
                BigDecimal.valueOf(500000), BigDecimal.valueOf(100000), null,
                new BigDecimal("20.00"), null, BigDecimal.valueOf(100000), null, goal.getCreatedAt());

        when(service.internalListForUser("u1")).thenReturn(List.of(goal));
        when(goalMapper.toResponse(goal)).thenReturn(response);

        mvc.perform(get("/api/goals/internal/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trip"));
    }
}
