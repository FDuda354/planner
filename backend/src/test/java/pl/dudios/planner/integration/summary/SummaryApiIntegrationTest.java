package pl.dudios.planner.integration.summary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.transaction.model.TransactionType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@code /api/summary}: the database aggregation of transactions into monthly
 * and yearly totals and into savings-goal progress, run against real PostgreSQL.
 *
 * <p>Amounts are compared as numbers taken from the JSON response.</p>
 */
class SummaryApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ROOT = "/api/summary";

  private Member filip;
  private Member klaudia;
  private ExpenseCategory food;
  private ExpenseCategory fun;
  private IncomeSource salary;
  private SavingsGoal japan;

  @BeforeEach
  void setUpHousehold() {
    Household household = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
    food = fixtures.expenseCategory(household, "Jedzenie", true);
    fun = fixtures.expenseCategory(household, "Rozrywka", true);
    salary = fixtures.incomeSource(household, "Wynagrodzenie");
    japan = fixtures.savingsGoal(household, "Wakacje w Japonii", "2000");
  }

  @Test
  @DisplayName("month summary totals each type and splits personal spending per member, leaving shared expenses aside")
  void summarizesMonth() throws Exception {
    fixtures.transaction(filip, TransactionType.INCOME, salary.getId(), "8000.00", LocalDate.of(2026, 9, 10), false);
    fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "300.00", LocalDate.of(2026, 9, 3), true);
    fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "200.00", LocalDate.of(2026, 9, 17), true);
    fixtures.transaction(filip, TransactionType.EXPENSE, fun.getId(), "90.00", LocalDate.of(2026, 9, 5), false);
    fixtures.transaction(klaudia, TransactionType.EXPENSE, fun.getId(), "60.00", LocalDate.of(2026, 9, 6), false);
    fixtures.transaction(klaudia, TransactionType.EXPENSE, fun.getId(), "90.00", LocalDate.of(2026, 9, 30), false);
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "1000.00", LocalDate.of(2026, 9, 11), false);
    fixtures.transaction(filip, TransactionType.EXPENSE, fun.getId(), "15.00", LocalDate.of(2026, 8, 31), false);

    mockMvc.perform(get(ROOT + "/months").with(auth.as(filip)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[0].month").value("2026-09"))
           .andExpect(jsonPath("$[0].income").value(8000.0))
           .andExpect(jsonPath("$[0].expenses").value(740.0))
           .andExpect(jsonPath("$[0].savings").value(1000.0))
           .andExpect(jsonPath("$[0].sharedExpenses").value(500.0))
           .andExpect(jsonPath("$[0].expensesByCategory[0].itemId").value(food.getId()))
           .andExpect(jsonPath("$[0].expensesByCategory[0].amount").value(500.0))
           .andExpect(jsonPath("$[0].personalExpensesByMember", hasSize(2)))
           .andExpect(jsonPath("$[0].personalExpensesByMember[0].memberId").value(klaudia.getId()))
           .andExpect(jsonPath("$[0].personalExpensesByMember[0].amount").value(150.0))
           .andExpect(jsonPath("$[0].personalExpensesByMember[1].memberId").value(filip.getId()))
           .andExpect(jsonPath("$[0].personalExpensesByMember[1].amount").value(90.0))
           .andExpect(jsonPath("$[1].month").value("2026-08"))
           .andExpect(jsonPath("$[1].expenses").value(15.0));
  }

  @Test
  @DisplayName("year summary has twelve months and leaves out transactions from other years")
  void summarizesYear() throws Exception {
    fixtures.transaction(filip, TransactionType.INCOME, salary.getId(), "8000.00", LocalDate.of(2026, 1, 1), false);
    fixtures.transaction(filip, TransactionType.INCOME, salary.getId(), "8100.00", LocalDate.of(2026, 12, 31), false);
    fixtures.transaction(filip, TransactionType.INCOME, salary.getId(), "7000.00", LocalDate.of(2025, 12, 31), false);
    fixtures.transaction(filip, TransactionType.INCOME, salary.getId(), "7500.00", LocalDate.of(2027, 1, 1), false);

    mockMvc.perform(get(ROOT + "/year").param("year", "2026").with(auth.as(filip)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(12)))
           .andExpect(jsonPath("$[0].income").value(8000.0))
           .andExpect(jsonPath("$[5].income").value(0))
           .andExpect(jsonPath("$[11].income").value(8100.0));
  }

  @Test
  @DisplayName("refuses a year outside the supported range with HTTP 400")
  void refusesUnsupportedYear() throws Exception {
    mockMvc.perform(get(ROOT + "/year").param("year", "1999").with(auth.as(filip)))
           .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("goal progress adds every deposit to the starting amount")
  void reportsGoalProgress() throws Exception {
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "500.00", LocalDate.of(2026, 8, 1), false);
    fixtures.transaction(klaudia, TransactionType.SAVING, japan.getId(), "300.00", LocalDate.of(2026, 8, 20), false);
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "1000.00", LocalDate.of(2026, 9, 1), false);

    mockMvc.perform(get(ROOT + "/goals").with(auth.as(klaudia)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].goal.id").value(japan.getId()))
           .andExpect(jsonPath("$[0].saved").value(3800.0))
           .andExpect(jsonPath("$[0].contributions", hasSize(2)))
           .andExpect(jsonPath("$[0].contributions[0].month").value("2026-08"))
           .andExpect(jsonPath("$[0].contributions[0].amount").value(800.0));
  }

  @Test
  @DisplayName("never mixes in transactions of another household")
  void isolatesHouseholds() throws Exception {
    var otherHousehold = fixtures.household();
    var stranger = fixtures.member(otherHousehold, "Obcy", "obcy@planner.test");
    var strangerFood = fixtures.expenseCategory(otherHousehold, "Jedzenie", true);
    fixtures.transaction(stranger, TransactionType.EXPENSE, strangerFood.getId(), "999.00", LocalDate.of(2026, 9, 1), false);
    fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "10.00", LocalDate.of(2026, 9, 1), false);

    mockMvc.perform(get(ROOT + "/months").with(auth.as(filip)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].expenses").value(10.0));
  }

}
