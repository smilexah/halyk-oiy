package kz.halyk.maqsat.alser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.dto.DeviceOfferDto;
import kz.halyk.maqsat.alser.service.OfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {OfferController.class, ApplyDiscountController.class},
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class OfferControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OfferService offerService;

    @Test
    void getOffersReturnsJsonArray() throws Exception {
        UUID offerId = UUID.randomUUID();
        DeviceOfferDto dto = new DeviceOfferDto(
                offerId, "TEST-SKU", "Test Device",
                new BigDecimal("100000"), new BigDecimal("10.00"),
                List.of("electronics-saver"), Instant.now().plusSeconds(86400)
        );
        when(offerService.listAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/alser/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sku").value("TEST-SKU"))
                .andExpect(jsonPath("$[0].name").value("Test Device"));
    }

    @Test
    void postApplyDiscountReturns200() throws Exception {
        UUID offerId = UUID.randomUUID();
        UUID appliedId = UUID.randomUUID();
        ApplyDiscountResponse response = new ApplyDiscountResponse(
                appliedId, offerId, "user-abc",
                new BigDecimal("100000"), new BigDecimal("10.00"),
                new BigDecimal("90000.00"), Instant.now()
        );
        when(offerService.apply(any(ApplyDiscountRequest.class))).thenReturn(response);

        ApplyDiscountRequest req = new ApplyDiscountRequest("user-abc", offerId);

        mockMvc.perform(post("/api/alser/apply-discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-abc"))
                .andExpect(jsonPath("$.finalPrice").value(90000.00));
    }
}
