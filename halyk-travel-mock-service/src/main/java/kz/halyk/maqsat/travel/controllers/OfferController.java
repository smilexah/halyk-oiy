package kz.halyk.maqsat.travel.controllers;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;
import kz.halyk.maqsat.travel.services.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/halyk-travel")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/offers")
    public List<TravelOfferDto> list(@RequestParam(required = false) List<String> audience) {
        return offerService.listByAudience(audience);
    }

    @GetMapping("/offers/{id}")
    public TravelOfferDto get(@PathVariable UUID id) {
        return offerService.getById(id);
    }
}
