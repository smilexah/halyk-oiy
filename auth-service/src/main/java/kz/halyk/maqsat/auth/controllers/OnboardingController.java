package kz.halyk.maqsat.auth.controllers;

import jakarta.validation.Valid;
import kz.halyk.maqsat.auth.dto.req.InviteRequest;
import kz.halyk.maqsat.auth.dto.res.InviteResponse;
import kz.halyk.maqsat.auth.services.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/invite")
    public ResponseEntity<InviteResponse> invite(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody InviteRequest request) {
        String initiatorBearer = jwt.getTokenValue();
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.invite(initiatorBearer, request));
    }
}
