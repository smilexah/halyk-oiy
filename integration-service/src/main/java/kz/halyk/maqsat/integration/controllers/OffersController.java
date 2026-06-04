package kz.halyk.maqsat.integration.controllers;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration")
public class OffersController {

    public record Offer(String partner, String title, String reward) {}

    @GetMapping("/offers")
    public List<Offer> offers() {
        return List.of(
                new Offer("Magnum", "Продукты со скидкой", "5% кешбэк"),
                new Offer("Yandex.Go", "Поездки", "10% бонусами"),
                new Offer("Halyk Maqsat", "Накопления на цель", "+2% к ставке")
        );
    }
}
