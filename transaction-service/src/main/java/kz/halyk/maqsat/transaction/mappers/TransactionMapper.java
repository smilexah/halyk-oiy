package kz.halyk.maqsat.transaction.mappers;

import kz.halyk.maqsat.transaction.dto.res.TransactionResponse;
import kz.halyk.maqsat.transaction.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "status", expression = "java(t.getStatus() != null ? t.getStatus().name() : null)")
    @Mapping(target = "direction", expression = "java(t.getDirection() != null ? t.getDirection().name() : null)")
    @Mapping(target = "operationType", expression = "java(t.getOperationType() != null ? t.getOperationType().name() : null)")
    TransactionResponse toResponse(Transaction t);
}
