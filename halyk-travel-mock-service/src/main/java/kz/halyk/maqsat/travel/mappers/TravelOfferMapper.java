package kz.halyk.maqsat.travel.mappers;

import kz.halyk.maqsat.travel.dto.res.TravelOfferDto;
import kz.halyk.maqsat.travel.entities.TravelOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TravelOfferMapper {

    @Mapping(target = "kind", expression = "java(entity.getKind().name())")
    TravelOfferDto toDto(TravelOffer entity);
}
