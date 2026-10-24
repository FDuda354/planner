package pl.dudios.planner.budgetconfig.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DefaultBudgetConfig {

  record DefaultExpenseCategory(String name, String icon, String color) {

  }

  record DefaultIncomeSource(String name, String icon, String color) {

  }

  record DefaultSavingsGoal(String name, String icon, String color, String note, BigDecimal targetAmount) {

  }

  static final List<DefaultExpenseCategory> EXPENSE_CATEGORIES = List.of(
    new DefaultExpenseCategory("Mieszkanie", "pi-home", "#6366f1"),
    new DefaultExpenseCategory("Rachunki", "pi-bolt", "#f59e0b"),
    new DefaultExpenseCategory("Jedzenie", "pi-shopping-cart", "#10b981"),
    new DefaultExpenseCategory("Transport", "pi-car", "#3b82f6"),
    new DefaultExpenseCategory("Na mieście", "pi-face-smile", "#f97316"),
    new DefaultExpenseCategory("Zdrowie", "pi-heart", "#ec4899"),
    new DefaultExpenseCategory("Rozrywka", "pi-ticket", "#8b5cf6"),
    new DefaultExpenseCategory("Subskrypcje", "pi-video", "#64748b"),
    new DefaultExpenseCategory("Ubrania", "pi-shopping-bag", "#d946ef"),
    new DefaultExpenseCategory("Dom i chemia", "pi-wrench", "#78716c"),
    new DefaultExpenseCategory("Prezenty", "pi-gift", "#e11d48"),
    new DefaultExpenseCategory("Podróże", "pi-globe", "#06b6d4")
  );

  static final List<DefaultIncomeSource> INCOME_SOURCES = List.of(
    new DefaultIncomeSource("Wynagrodzenie", "pi-briefcase", "#059669"),
    new DefaultIncomeSource("Premie", "pi-trophy", "#ca8a04"),
    new DefaultIncomeSource("Inne wpływy", "pi-wallet", "#65a30d")
  );

  static final List<DefaultSavingsGoal> SAVINGS_GOALS = List.of(
    new DefaultSavingsGoal("Poduszka finansowa", "pi-shield", "#0ea5e9", "Kilka miesięcy spokojnego życia", new BigDecimal("20000"))
  );

}
