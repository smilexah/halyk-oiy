package kz.halyk.maqsat.summary.services;

import org.springframework.stereotype.Component;

@Component
public class LanguagePicker {
    public String pick(String userId) {
        return "ru";
    }
}
