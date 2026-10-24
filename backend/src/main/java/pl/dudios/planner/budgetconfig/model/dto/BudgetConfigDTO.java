package pl.dudios.planner.budgetconfig.model.dto;

import java.util.List;

public record BudgetConfigDTO(
  List<ExpenseCategoryDTO> expenseCategories,
  List<IncomeSourceDTO> incomeSources,
  List<SavingsGoalDTO> savingsGoals
) {

}
