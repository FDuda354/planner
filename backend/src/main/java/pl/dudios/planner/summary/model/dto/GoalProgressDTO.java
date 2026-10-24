package pl.dudios.planner.summary.model.dto;

import pl.dudios.planner.budgetconfig.model.dto.SavingsGoalDTO;

import java.math.BigDecimal;
import java.util.List;

public record GoalProgressDTO(
  SavingsGoalDTO goal,
  BigDecimal saved,
  List<MonthlyContributionDTO> contributions
) {

}
