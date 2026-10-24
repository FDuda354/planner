package pl.dudios.planner.summary.model.dto;

import java.math.BigDecimal;

public record MonthlyContributionDTO(
  String month,
  BigDecimal amount
) {

}
