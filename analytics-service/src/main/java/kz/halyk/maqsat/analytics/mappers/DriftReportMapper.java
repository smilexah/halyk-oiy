package kz.halyk.maqsat.analytics.mappers;

import kz.halyk.maqsat.analytics.dto.res.DriftReportResponse;
import kz.halyk.maqsat.analytics.entities.DriftReport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriftReportMapper {

    DriftReportResponse toResponse(DriftReport entity);
}
