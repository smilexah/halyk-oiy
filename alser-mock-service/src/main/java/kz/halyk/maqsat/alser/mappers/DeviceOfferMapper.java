package kz.halyk.maqsat.alser.mappers;

import kz.halyk.maqsat.alser.dto.res.DeviceOfferDto;
import kz.halyk.maqsat.alser.entities.DeviceOffer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeviceOfferMapper {

    DeviceOfferDto toDto(DeviceOffer entity);
}
