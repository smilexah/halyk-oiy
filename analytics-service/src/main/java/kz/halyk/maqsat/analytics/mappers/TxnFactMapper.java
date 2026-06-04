package kz.halyk.maqsat.analytics.mappers;

import kz.halyk.maqsat.analytics.entities.TxnFact;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TxnFactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ingestedAt", ignore = true)
    @Mapping(target = "currency", expression = "java(event.currency() != null ? event.currency() : \"KZT\")")
    TxnFact toEntity(TransactionCategorized event);
}
