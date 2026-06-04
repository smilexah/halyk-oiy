package kz.halyk.maqsat.travel.controllers;

import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;
import kz.halyk.maqsat.travel.services.OfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OfferController.class, BookingController.class})
class OfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OfferService offerService;

    @Test
    void listOffersReturnsAll() throws Exception {
        UUID id = UUID.randomUUID();
        TravelOfferDto dto = new TravelOfferDto(
                id, "FLIGHT", "Astana → Antalya",
                new BigDecimal("180000"), new BigDecimal("15.00"),
                List.of("saving_for_trip", "beach-lover"),
                Instant.now().plusSeconds(86400)
        );
        when(offerService.listByAudience(isNull())).thenReturn(List.of(dto));
        when(offerService.listByAudience(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/halyk-travel/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("FLIGHT"))
                .andExpect(jsonPath("$[0].destination").value("Astana → Antalya"));
    }

    @Test
    void getOfferByIdReturnsDto() throws Exception {
        UUID id = UUID.randomUUID();
        TravelOfferDto dto = new TravelOfferDto(
                id, "HOTEL", "Dubai 5★",
                new BigDecimal("420000"), new BigDecimal("20.00"),
                List.of("luxury"),
                Instant.now().plusSeconds(86400)
        );
        when(offerService.getById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/halyk-travel/offers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("HOTEL"))
                .andExpect(jsonPath("$.destination").value("Dubai 5★"));
    }
}
