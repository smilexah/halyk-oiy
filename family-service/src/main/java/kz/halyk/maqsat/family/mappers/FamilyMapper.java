package kz.halyk.maqsat.family.mappers;

import kz.halyk.maqsat.family.dto.res.GroupResponse;
import kz.halyk.maqsat.family.entities.FamilyGroup;
import kz.halyk.maqsat.family.entities.Membership;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FamilyMapper {

    @Mapping(source = "id", target = "membershipId")
    @Mapping(target = "role", expression = "java(m.getRole().name())")
    @Mapping(source = "childLimit.dailyLimit", target = "dailyLimit")
    GroupResponse.MemberView toMemberView(Membership m);

    default GroupResponse toGroupResponse(FamilyGroup g) {
        return new GroupResponse(
                g.getId(), g.getName(), g.getType(), g.getCreatedBy(), g.getCreatedAt(),
                g.getMemberships().stream().map(this::toMemberView).toList());
    }
}
