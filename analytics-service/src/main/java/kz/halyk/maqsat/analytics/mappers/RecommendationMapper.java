package kz.halyk.maqsat.analytics.mappers;

import kz.halyk.maqsat.analytics.dto.req.RecommendationBatch;
import kz.halyk.maqsat.analytics.dto.res.RecommendationDto;
import kz.halyk.maqsat.analytics.entities.Recommendation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    RecommendationDto toDto(Recommendation entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "period", target = "period")
    Recommendation toEntity(RecommendationBatch.Item item, String userId, String period);
}
