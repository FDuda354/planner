package pl.dudios.planner.transaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.dto.ItemUsageDTO;
import pl.dudios.planner.transaction.model.dto.TransactionDTO;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static pl.dudios.planner.utils.AppTimeZone.EARLIEST_ON_EARTH;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final TransactionDetailsValidator transactionDetailsValidator;

  @Transactional(readOnly = true)
  public List<TransactionDTO> getTransactions(Long householdId, YearMonth month) {
    return transactionRepository.findAllByHouseholdIdAndDateBetweenOrderByDateDescIdDesc(householdId, month.atDay(1), month.atEndOfMonth())
                                .stream()
                                .map(Transaction::toDTO)
                                .toList();
  }

  @Transactional
  public void saveTransaction(Long householdId, TransactionRequest request) {
    Transaction transaction = request.id() == null
      ? Transaction.builder().householdId(householdId).build()
      : getTransaction(householdId, request.id());

    validateDate(request.date());
    transactionDetailsValidator.validate(householdId, request, transaction.getMemberId());

    transaction.assignItem(request.type(), request.itemId());
    transaction.setAmount(request.amount());
    transaction.setDate(request.date());
    transaction.setMemberId(request.memberId());
    transaction.setShared(request.shared());
    transaction.setNote(request.note());

    transactionRepository.save(transaction);
  }

  @Transactional
  public void deleteTransaction(Long householdId, Long transactionId) {
    transactionRepository.delete(getTransaction(householdId, transactionId));
  }

  @Transactional(readOnly = true)
  public ItemUsageDTO getItemUsage(Long householdId, TransactionType type, Long itemId) {
    return transactionRepository.findItemUsage(householdId, type, itemId);
  }

  @Transactional
  public void deleteItemTransactions(Long householdId, TransactionType type, Long itemId) {
    transactionRepository.deleteAllByItem(householdId, type, itemId);
  }

  @Transactional
  public void deleteHouseholdTransactions(Long householdId) {
    transactionRepository.deleteAllByHouseholdId(householdId);
  }

  private Transaction getTransaction(Long householdId, Long transactionId) {
    return transactionRepository.findByIdAndHouseholdId(transactionId, householdId)
                                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono transakcji"));
  }

  private void validateDate(LocalDate date) {
    if (date.isAfter(YearMonth.now(EARLIEST_ON_EARTH).atEndOfMonth())) {
      throw new RequestValidationException("Transakcję można zaplanować najpóźniej na koniec bieżącego miesiąca");
    }
  }

}
