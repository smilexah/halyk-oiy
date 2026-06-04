package kz.halyk.maqsat.alser.controllers;

import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.alser.dto.res.DeviceOfferDto;
import kz.halyk.maqsat.alser.services.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alser")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService service;

    @GetMapping("/offers")
    public List<DeviceOfferDto> list(@RequestParam(required = false) List<String> audience) {
        if (audience == null || audience.isEmpty()) {
            return service.listAll();
        }
        return service.matchingAudience(audience);
    }

    @GetMapping("/offers/{id}")
    public DeviceOfferDto get(@PathVariable UUID id) {
        return service.get(id);
    }
}
