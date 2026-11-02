package pl.dudios.planner.recurring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.model.BudgetItem;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.model.dto.RecurringTransactionDTO;
import pl.dudios.planner.recurring.model.request.RecurringTransactionRequest;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.transaction.model.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static pl.dudios.planner.utils.AppTimeZone.POLAND;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

  private final RecurringTransactionRepository recurringTransactionRepository;
  private final RecurringTransactionGenerator generator;
  private final BudgetConfigService budgetConfigService;
  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public List<RecurringTransactionDTO> getRecurringTransactions(Long householdId) {
    return recurringTransactionRepository.findAllByHouseholdIdOrderByDayOfMonthAscIdAsc(householdId).stream()
                                         .map(RecurringTransaction::toDTO)
                                         .toList();
  }

  @Transactional
  public void saveRecurringTransaction(Long householdId, RecurringTransactionRequest request) {
    RecurringTransaction recurring = request.id() == null
      ? RecurringTransaction.builder().householdId(householdId).build()
      : getRecurringTransaction(householdId, request.id());

    validateItem(householdId, recurring, request);
    validateMember(householdId, recurring, request.memberId());
    validateSharedFlag(request);

    LocalDate today = LocalDate.now(POLAND);
    boolean needsScheduling = needsScheduling(recurring, request);
    recurring.assignItem(request.type(), request.itemId());
    recurring.setAmount(request.amount());
    recurring.setDayOfMonth(request.dayOfMonth());
    recurring.setMemberId(request.memberId());
    recurring.setShared(request.shared());
    recurring.setNote(request.note());
    recurring.setActive(request.active());
    if (needsScheduling) {
      recurring.scheduleFrom(today);
    }

    generator.generateDue(recurringTransactionRepository.save(recurring), today);
  }

  @Transactional
  public void deleteRecurringTransaction(Long householdId, Long recurringTransactionId) {
    recurringTransactionRepository.delete(getRecurringTransaction(householdId, recurringTransactionId));
  }

  private RecurringTransaction getRecurringTransaction(Long householdId, Long recurringTransactionId) {
    return recurringTransactionRepository.findByIdAndHouseholdId(recurringTransactionId, householdId)
                                         .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono stałej transakcji"));
  }

  private boolean needsScheduling(RecurringTransaction recurring, RecurringTransactionRequest request) {
    boolean isNew = recurring.getNextOccurrence() == null;
    boolean dayChanged = recurring.getDayOfMonth() != request.dayOfMonth();
    boolean resumed = request.active() && !recurring.isActive();
    return isNew || dayChanged || resumed;
  }

  private void validateItem(Long householdId, RecurringTransaction recurring, RecurringTransactionRequest request) {
    BudgetItem item = budgetConfigService.getItem(householdId, request.type(), request.itemId());
    boolean keepsCurrentItem = recurring.getType() == request.type() && Objects.equals(recurring.getItemId(), request.itemId());
    if (!item.isActive() && !keepsCurrentItem) {
      throw new RequestValidationException("Pozycja „" + item.getName() + "” jest wstrzymana — przywróć ją w konfiguracji, aby ustawić stałą transakcję");
    }
  }

  private void validateMember(Long householdId, RecurringTransaction recurring, Long memberId) {
    Member member = memberRepository.findByIdAndHouseholdId(memberId, householdId)
                                    .orElseThrow(() -> new RequestValidationException("Wybrana osoba nie należy do Waszego budżetu"));
    if (!member.isActive() && !Objects.equals(recurring.getMemberId(), memberId)) {
      throw new RequestValidationException("Nie można przypisać stałej transakcji osobie usuniętej z budżetu");
    }
  }

  private void validateSharedFlag(RecurringTransactionRequest request) {
    if (request.shared() && request.type() != TransactionType.EXPENSE) {
      throw new RequestValidationException("Na dobro wspólne można oznaczyć tylko wydatek");
    }
  }

}
