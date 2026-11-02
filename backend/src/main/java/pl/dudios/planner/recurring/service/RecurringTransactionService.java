package pl.dudios.planner.recurring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.recurring.model.RecurrenceFrequency;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.model.dto.RecurringTransactionDTO;
import pl.dudios.planner.recurring.model.request.RecurringTransactionRequest;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.transaction.service.TransactionDetailsValidator;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;

import static pl.dudios.planner.utils.AppTimeZone.POLAND;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

  private final RecurringTransactionRepository recurringTransactionRepository;
  private final RecurringTransactionGenerator generator;
  private final TransactionDetailsValidator transactionDetailsValidator;

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

    validateSchedule(request);
    transactionDetailsValidator.validate(householdId, request, recurring.getMemberId());

    LocalDate today = LocalDate.now(POLAND);
    boolean needsScheduling = needsScheduling(recurring, request);
    recurring.assignItem(request.type(), request.itemId());
    recurring.setAmount(request.amount());
    recurring.setFrequency(request.frequency());
    recurring.setDayOfMonth(request.dayOfMonth());
    recurring.setMonthOfYear(monthOfYearOf(request));
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
    boolean scheduleChanged = hasScheduleChanged(recurring, request);
    boolean resumed = request.active() && !recurring.isActive();
    return isNew || scheduleChanged || resumed;
  }

  private boolean hasScheduleChanged(RecurringTransaction recurring, RecurringTransactionRequest request) {
    return recurring.getFrequency() != request.frequency()
           || recurring.getDayOfMonth() != request.dayOfMonth()
           || !Objects.equals(recurring.getMonthOfYear(), monthOfYearOf(request));
  }

  private void validateSchedule(RecurringTransactionRequest request) {
    if (request.frequency() != RecurrenceFrequency.YEARLY) {
      return;
    }
    if (request.monthOfYear() == null) {
      throw new RequestValidationException("Wybierz miesiąc rocznego naliczania");
    }
    if (request.dayOfMonth() > Month.of(request.monthOfYear()).maxLength()) {
      throw new RequestValidationException("Wybrany miesiąc nie ma tylu dni");
    }
  }

  private Integer monthOfYearOf(RecurringTransactionRequest request) {
    return request.frequency() == RecurrenceFrequency.YEARLY ? request.monthOfYear() : null;
  }

}
