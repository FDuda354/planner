package pl.dudios.planner.summary.model.dto;

import java.math.BigDecimal;

public record ItemAmountDTO(
  Long itemId,
  BigDecimal amount
) {

}
