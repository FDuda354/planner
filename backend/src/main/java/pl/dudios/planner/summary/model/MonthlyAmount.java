package pl.dudios.planner.summary.model;

import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAmount(
  Integer year,
  Integer month,
  TransactionType type,
  Long itemId,
  Long memberId,
  Boolean shared,
  BigDecimal amount
) {

  public YearMonth yearMonth() {
    return YearMonth.of(year, month);
  }

  public boolean isPersonalExpense() {
    return type == TransactionType.EXPENSE && !shared;
  }

  public boolean isSharedExpense() {
    return type == TransactionType.EXPENSE && shared;
  }

}
