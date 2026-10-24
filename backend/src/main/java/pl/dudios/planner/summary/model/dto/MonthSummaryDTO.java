package pl.dudios.planner.summary.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthSummaryDTO(
  String month,
  BigDecimal income,
  BigDecimal expenses,
  BigDecimal savings,
  List<ItemAmountDTO> expensesByCategory,
  List<ItemAmountDTO> incomesBySource,
  List<ItemAmountDTO> savingsByGoal,
  List<MemberAmountDTO> personalExpensesByMember,
  BigDecimal sharedExpenses
) {

}
