package kz.halyk.maqsat.alser.controllers;

import jakarta.validation.Valid;
import kz.halyk.maqsat.alser.dto.req.ApplyDiscountRequest;
import kz.halyk.maqsat.alser.dto.res.ApplyDiscountResponse;
import kz.halyk.maqsat.alser.services.OfferService;
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
