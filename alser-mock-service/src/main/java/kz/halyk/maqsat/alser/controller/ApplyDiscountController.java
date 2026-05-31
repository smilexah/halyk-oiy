package kz.halyk.maqsat.alser.controller;

import jakarta.validation.Valid;
import kz.halyk.maqsat.alser.dto.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alser")
@RequiredArgsConstructor
public class ApplyDiscountController {

    private final OfferService service;

    @PostMapping("/apply-discount")
    public ApplyDiscountResponse apply(@Valid @RequestBody ApplyDiscountRequest req) {
        return service.apply(req);
    }
}
