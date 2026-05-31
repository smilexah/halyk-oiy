package kz.halyk.maqsat.summary.service;

import org.springframework.stereotype.Component;

@Component
public class LanguagePicker {
    // MVP — TODO: user preference store
    public String pick(String userId) {
        return "ru";
    }
}
