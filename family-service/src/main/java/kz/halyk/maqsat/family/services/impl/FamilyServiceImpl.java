package kz.halyk.maqsat.family.services.impl;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kz.halyk.maqsat.family.dto.req.AddMemberRequest;
import kz.halyk.maqsat.family.dto.res.ApprovalResponse;
import kz.halyk.maqsat.family.dto.res.ChildLimitResponse;
import kz.halyk.maqsat.family.dto.res.GroupResponse;
import kz.halyk.maqsat.family.entities.ChildLimit;
import kz.halyk.maqsat.family.entities.FamilyGroup;
import kz.halyk.maqsat.family.entities.Membership;
import kz.halyk.maqsat.family.entities.enums.Role;
import kz.halyk.maqsat.family.event.FamilyEventPublisher;
import kz.halyk.maqsat.family.exceptions.NotFoundException;
import kz.halyk.maqsat.family.mappers.FamilyMapper;
import kz.halyk.maqsat.family.repositories.FamilyGroupRepository;
import kz.halyk.maqsat.family.repositories.MembershipRepository;
import kz.halyk.maqsat.family.services.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyGroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final FamilyEventPublisher eventPublisher;
    private final FamilyMapper familyMapper;
    private final MeterRegistry meterRegistry;

    @Override
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

        return familyMapper.toGroupResponse(groupRepository.save(group));
    }

    @Override
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

        return familyMapper.toGroupResponse(groupRepository.save(group));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> myGroups(String userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(Membership::getGroup)
                .collect(Collectors.toMap(FamilyGroup::getId, g -> g, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(familyMapper::toGroupResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroup(UUID groupId) {
        FamilyGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        return familyMapper.toGroupResponse(group);
    }

    @Override
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

        return familyMapper.toGroupResponse(member.getGroup());
    }

    @Override
    @Transactional(readOnly = true)
    public ChildLimitResponse getChildLimit(String userId) {
        Membership member = membershipRepository.findFirstByUserIdAndRole(userId, Role.CHILD)
                .orElseThrow(() -> new NotFoundException("No child membership for user: " + userId));
        ChildLimit limit = member.getChildLimit();
        if (limit == null) throw new NotFoundException("No daily limit set for user: " + userId);
        return new ChildLimitResponse(userId, limit.getDailyLimit(), limit.getOverrideUntil(), limit.getOverrideAmount());
    }

    @Override
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
