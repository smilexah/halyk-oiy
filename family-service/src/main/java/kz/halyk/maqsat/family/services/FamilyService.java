package kz.halyk.maqsat.family.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.family.dto.req.AddMemberRequest;
import kz.halyk.maqsat.family.dto.res.ApprovalResponse;
import kz.halyk.maqsat.family.dto.res.ChildLimitResponse;
import kz.halyk.maqsat.family.dto.res.GroupResponse;

public interface FamilyService {
    GroupResponse createGroup(String creatorUserId, String name);
    GroupResponse addMember(UUID groupId, AddMemberRequest request);
    List<GroupResponse> myGroups(String userId);
    GroupResponse getGroup(UUID groupId);
    GroupResponse setLimit(UUID membershipId, BigDecimal dailyLimit);
    ChildLimitResponse getChildLimit(String userId);
    ApprovalResponse approveOverride(UUID transactionId, String childUserId, BigDecimal approvedAmount, String approverUserId);
}
