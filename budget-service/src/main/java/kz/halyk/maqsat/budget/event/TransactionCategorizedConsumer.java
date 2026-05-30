package kz.halyk.maqsat.budget.event;

import java.time.ZoneOffset;
import kz.halyk.maqsat.budget.service.BudgetService;
import kz.halyk.maqsat.common.event.EventTopics;
import kz.halyk.maqsat.common.event.TransactionCategorized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCategorizedConsumer {

    private final BudgetService budgetService;

    @KafkaListener(topics = EventTopics.TRANSACTION_CATEGORIZED, groupId = "budget-service")
    public void onTransactionCategorized(TransactionCategorized event) {
        log.info("Received TransactionCategorized {} category={} amount={}",
                event.transactionId(), event.categoryName(), event.amount());
        budgetService.trackSpending(
                event.userId(),
                event.categoryName(),
                event.amount(),
                event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate());
    }
}