package kz.halyk.maqsat.auth.services;

import kz.halyk.maqsat.auth.dto.req.InviteRequest;
import kz.halyk.maqsat.auth.dto.res.InviteResponse;

public interface OnboardingService {
    InviteResponse invite(String initiatorBearer, InviteRequest request);
}
