package pl.dudios.planner.recurring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.time.LocalDate;
import java.util.List;

import static pl.dudios.planner.utils.AppTimeZone.POLAND;
import static pl.dudios.planner.utils.AppTimeZone.POLAND_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionGenerator {

  private final RecurringTransactionRepository recurringTransactionRepository;
  private final TransactionRepository transactionRepository;
  private final MemberRepository memberRepository;

  @Transactional
  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(cron = "0 5 0 * * *", zone = POLAND_ID)
  public void generateDueTransactions() {
    LocalDate today = LocalDate.now(POLAND);
    List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findAllByActiveTrueAndNextOccurrenceLessThanEqual(today);
    dueTransactions.forEach(recurring -> generateDue(recurring, today));
    log.info("Processed {} due recurring transactions", dueTransactions.size());
  }

  public void generateDue(RecurringTransaction recurring, LocalDate today) {
    while (recurring.isDueOn(today)) {
      if (canBeBooked(recurring)) {
        transactionRepository.save(recurring.toTransaction());
      }
      recurring.advanceToNextOccurrence();
    }
  }

  private boolean canBeBooked(RecurringTransaction recurring) {
    return recurring.belongsToNobody() || memberRepository.existsByIdAndActiveTrue(recurring.getMemberId());
  }

}
