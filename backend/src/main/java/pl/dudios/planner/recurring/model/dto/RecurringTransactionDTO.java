package pl.dudios.planner.recurring.model.dto;

import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionDTO(
  Long id,
  TransactionType type,
  Long itemId,
  BigDecimal amount,
  int dayOfMonth,
  Long memberId,
  boolean shared,
  String note,
  boolean active,
  LocalDate nextOccurrence
) {

}
