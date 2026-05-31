package kz.halyk.maqsat.family.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kz.halyk.maqsat.family.domain.ChildLimit;
import kz.halyk.maqsat.family.domain.FamilyGroup;
import kz.halyk.maqsat.family.domain.Membership;
import kz.halyk.maqsat.family.domain.Role;
import kz.halyk.maqsat.family.dto.AddMemberRequest;
import kz.halyk.maqsat.family.dto.ApprovalResponse;
import kz.halyk.maqsat.family.dto.ChildLimitResponse;
import kz.halyk.maqsat.family.dto.GroupResponse;
import kz.halyk.maqsat.family.event.FamilyEventPublisher;
import kz.halyk.maqsat.family.exception.NotFoundException;
import kz.halyk.maqsat.family.repository.FamilyGroupRepository;
import kz.halyk.maqsat.family.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyGroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final FamilyEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @Transactional
    public GroupResponse createGroup(String creatorUserId, String name) {
        FamilyGroup group = new FamilyGroup();
        group.setName(name);
        group.setType("FAMILY");
        group.setCreatedBy(creatorUserId);
        group.setCreatedAt(Instant.now());

        Membership creator = new Membership();
        creator.setUserId(creatorUserId);
        creator.setRole(Role.ADULT);
        group.addMembership(creator);

        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, AddMemberRequest request) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        Membership member = new Membership();
        member.setUserId(request.userId());
        member.setRole(request.role());
        group.addMembership(member);

        if (request.role() == Role.CHILD && request.dailyLimit() != null) {
            ChildLimit limit = new ChildLimit();
            limit.setMembership(member);
            limit.setDailyLimit(request.dailyLimit());
            member.setChildLimit(limit);
        }

        return GroupResponse.from(groupRepository.save(group));
    }

    /** Groups the current user belongs to (any role) — lets the app discover the family without a known id. */
    @Transactional(readOnly = true)
    public List<GroupResponse> myGroups(String userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(Membership::getGroup)
                // dedupe by group id, preserve order
                .collect(Collectors.toMap(FamilyGroup::getId, g -> g, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(GroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        return GroupResponse.from(group);
    }

    @Transactional
    public GroupResponse setLimit(UUID membershipId, BigDecimal dailyLimit) {
        Membership member = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new NotFoundException("Membership not found: " + membershipId));

        ChildLimit limit = member.getChildLimit();
        if (limit == null) {
            limit = new ChildLimit();
            limit.setMembership(member);
            member.setChildLimit(limit);
        }
        limit.setDailyLimit(dailyLimit);
        membershipRepository.save(member);

        return GroupResponse.from(member.getGroup());
    }

    @Transactional(readOnly = true)
    public ChildLimitResponse getChildLimit(String userId) {
        Membership member = membershipRepository.findFirstByUserIdAndRole(userId, Role.CHILD)
                .orElseThrow(() -> new NotFoundException("No child membership for user: " + userId));
        ChildLimit limit = member.getChildLimit();
        if (limit == null) {
            throw new NotFoundException("No daily limit set for user: " + userId);
        }
        return new ChildLimitResponse(userId, limit.getDailyLimit(), limit.getOverrideUntil(), limit.getOverrideAmount());
    }

    /**
     * Parent approves a held transaction: raise the child's allowance for the rest of the day
     * and publish LimitOverrideApproved so transaction-service can post the payment.
     */
    @Transactional
    public ApprovalResponse approveOverride(UUID transactionId, String childUserId, BigDecimal approvedAmount, String approverUserId) {
        Membership child = membershipRepository.findFirstByUserIdAndRole(childUserId, Role.CHILD)
                .orElseThrow(() -> new NotFoundException("No child membership for user: " + childUserId));

        ChildLimit limit = child.getChildLimit();
        if (limit == null) {
            limit = new ChildLimit();
            limit.setMembership(child);
            limit.setDailyLimit(BigDecimal.ZERO);
            child.setChildLimit(limit);
        }
        Instant endOfDay = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
                .plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal currentOverride = limit.getOverrideAmount() != null ? limit.getOverrideAmount() : BigDecimal.ZERO;
        limit.setOverrideUntil(endOfDay);
        limit.setOverrideAmount(currentOverride.add(approvedAmount));
        membershipRepository.save(child);

        eventPublisher.publishOverrideApproved(transactionId, childUserId, approvedAmount, approverUserId);
        meterRegistry.counter("maqsat.overrides.approved").increment();
        return new ApprovalResponse(transactionId, childUserId, approvedAmount, approverUserId, endOfDay);
    }
}
