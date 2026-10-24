package pl.dudios.planner.transaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.model.BudgetItem;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.dto.TransactionDTO;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import static pl.dudios.planner.utils.AppTimeZone.EARLIEST_ON_EARTH;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final BudgetConfigService budgetConfigService;

  @Transactional(readOnly = true)
  public List<TransactionDTO> getTransactions(Long householdId, YearMonth month) {
    return transactionRepository.findAllByHouseholdIdAndDateBetweenOrderByDateDescIdDesc(householdId, month.atDay(1), month.atEndOfMonth())
                                .stream()
                                .map(Transaction::toDTO)
                                .toList();
  }

  @Transactional
  public void saveTransaction(Member author, TransactionRequest request) {
    Long householdId = author.getHouseholdId();
    Transaction transaction = request.id() == null
      ? Transaction.builder().householdId(householdId).memberId(author.getId()).build()
      : getTransaction(householdId, request.id());

    validateItem(householdId, transaction, request);
    validateDate(request.date());
    validateSharedFlag(request);

    transaction.assignItem(request.type(), request.itemId());
    transaction.setAmount(request.amount());
    transaction.setDate(request.date());
    transaction.setShared(request.shared());
    transaction.setNote(request.note());

    transactionRepository.save(transaction);
  }

  @Transactional
  public void deleteTransaction(Long householdId, Long transactionId) {
    transactionRepository.delete(getTransaction(householdId, transactionId));
  }

  private Transaction getTransaction(Long householdId, Long transactionId) {
    return transactionRepository.findByIdAndHouseholdId(transactionId, householdId)
                                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono transakcji"));
  }

  private void validateItem(Long householdId, Transaction transaction, TransactionRequest request) {
    BudgetItem item = switch (request.type()) {
      case EXPENSE -> budgetConfigService.getExpenseCategory(householdId, request.itemId());
      case INCOME -> budgetConfigService.getIncomeSource(householdId, request.itemId());
      case SAVING -> budgetConfigService.getSavingsGoal(householdId, request.itemId());
    };
    boolean keepsCurrentItem = transaction.getType() == request.type() && Objects.equals(transaction.getItemId(), request.itemId());
    if (!item.isActive() && !keepsCurrentItem) {
      throw new RequestValidationException("Pozycja „" + item.getName() + "” jest wstrzymana — przywróć ją w konfiguracji, aby dodawać transakcje");
    }
  }

  private void validateDate(LocalDate date) {
    if (date.isAfter(LocalDate.now(EARLIEST_ON_EARTH))) {
      throw new RequestValidationException("Data transakcji nie może być z przyszłości");
    }
  }

  private void validateSharedFlag(TransactionRequest request) {
    if (request.shared() && request.type() != TransactionType.EXPENSE) {
      throw new RequestValidationException("Na dobro wspólne można oznaczyć tylko wydatek");
    }
  }

}
