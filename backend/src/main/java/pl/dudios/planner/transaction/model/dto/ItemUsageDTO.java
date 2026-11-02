package pl.dudios.planner.transaction.model.dto;

import java.math.BigDecimal;

public record ItemUsageDTO(
  Long transactionCount,
  BigDecimal totalAmount
) {

  public ItemUsageDTO {
    totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
  }

}
