package pl.dudios.planner.transaction.model.dto;

import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDTO(
  Long id,
  TransactionType type,
  Long itemId,
  BigDecimal amount,
  LocalDate date,
  Long memberId,
  boolean shared,
  String note
) {

}
