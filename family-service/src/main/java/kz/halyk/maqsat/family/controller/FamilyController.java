package kz.halyk.maqsat.family.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kz.halyk.maqsat.common.security.CurrentUser;
import kz.halyk.maqsat.family.dto.AddMemberRequest;
import kz.halyk.maqsat.family.dto.ApprovalRequest;
import kz.halyk.maqsat.family.dto.ApprovalResponse;
import kz.halyk.maqsat.family.dto.ChildLimitResponse;
import kz.halyk.maqsat.family.dto.CreateGroupRequest;
import kz.halyk.maqsat.family.dto.GroupResponse;
import kz.halyk.maqsat.family.dto.SetLimitRequest;
import kz.halyk.maqsat.family.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService service;

    @PostMapping("/groups")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        String userId = CurrentUser.current().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(userId, request.name()));
    }

    @PostMapping("/groups/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMember(id, request));
    }

    /** Groups the authenticated user belongs to — discovery for the app. */
    @GetMapping("/groups")
    public List<GroupResponse> myGroups() {
        return service.myGroups(CurrentUser.current().userId());
    }

    @GetMapping("/groups/{id}")
    public GroupResponse getGroup(@PathVariable UUID id) {
        return service.getGroup(id);
    }

    @PutMapping("/members/{id}/limit")
    public GroupResponse setLimit(@PathVariable UUID id, @Valid @RequestBody SetLimitRequest request) {
        return service.setLimit(id, request.dailyLimit());
    }

    @GetMapping("/members/by-user/{userId}/limit")
    public ChildLimitResponse childLimit(@PathVariable String userId) {
        return service.getChildLimit(userId);
    }

    /** Parent approves a held child transaction in one tap. */
    @PostMapping("/approvals/{transactionId}")
    public ApprovalResponse approve(@PathVariable UUID transactionId, @Valid @RequestBody ApprovalRequest request) {
        String approver = CurrentUser.current().userId();
        return service.approveOverride(transactionId, request.childUserId(), request.approvedAmount(), approver);
    }
}
