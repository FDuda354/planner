package pl.dudios.planner.summary.model.dto;

import java.math.BigDecimal;

public record MemberAmountDTO(
  Long memberId,
  BigDecimal amount
) {

}
