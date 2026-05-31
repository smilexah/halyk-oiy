package kz.halyk.maqsat.travel.controller;

import jakarta.validation.Valid;
import kz.halyk.maqsat.travel.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.travel.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.travel.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/halyk-travel")
@RequiredArgsConstructor
public class BookingController {

    private final OfferService offerService;

    @PostMapping("/apply-discount")
    public ApplyDiscountResponse apply(@Valid @RequestBody ApplyDiscountRequest req) {
        return offerService.apply(req);
    }
}
