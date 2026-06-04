package kz.halyk.maqsat.parsebudget.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.parsebudget.dto.req.BudgetPlanProposal;
import kz.halyk.maqsat.parsebudget.dto.req.PlannedCategory;
import kz.halyk.maqsat.parsebudget.dto.req.ReplanRequest;
import kz.halyk.maqsat.parsebudget.dto.res.ReplanResponse;
import kz.halyk.maqsat.parsebudget.services.ParseBudgetPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParseBudgetPlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParseBudgetPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParseBudgetPlanService service;

    @Test
    void validRequest_returns202WithBody() throws Exception {
        UUID newPlanId = UUID.randomUUID();
        UUID supersededId = UUID.randomUUID();
        ReplanResponse response = new ReplanResponse(newPlanId, 2, supersededId);

        when(service.handle(any())).thenReturn(response);

        ReplanRequest req = new ReplanRequest(
                "user-1", "2026-05",
                new BudgetPlanProposal(
                        List.of(new PlannedCategory("Продукты", "MANDATORY", new BigDecimal("100000"))),
                        "drift detected"),
                null);

        mockMvc.perform(post("/api/parse-budget/replan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.newPlanId").value(newPlanId.toString()))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.supersededPlanId").value(supersededId.toString()));
    }
}
