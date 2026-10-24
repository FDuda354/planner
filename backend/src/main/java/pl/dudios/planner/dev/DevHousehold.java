package pl.dudios.planner.dev;

import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;

import java.util.Map;

record DevHousehold(
  Long householdId,
  Long filipId,
  Long klaudiaId,
  Map<String, ExpenseCategory> expenseCategories,
  Map<String, IncomeSource> incomeSources,
  Map<String, SavingsGoal> savingsGoals
) {

  Long category(String key) {
    return expenseCategories.get(key).getId();
  }

  Long income(String key) {
    return incomeSources.get(key).getId();
  }

  Long goal(String key) {
    return savingsGoals.get(key).getId();
  }

}
