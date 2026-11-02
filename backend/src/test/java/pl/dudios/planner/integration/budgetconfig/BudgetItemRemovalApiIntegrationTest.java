package pl.dudios.planner.integration.budgetconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of deleting budget items ({@code /api/budget-config/item}) together with their
 * transactions, against real PostgreSQL with its foreign keys.
 */
class BudgetItemRemovalApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ITEM = "/api/budget-config/item";
  private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

  @Autowired
  private SavingsGoalRepository savingsGoalRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  private Household household;
  private Member filip;
  private SavingsGoal japan;

  @BeforeEach
  void setUpHousehold() {
    household = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    japan = fixtures.savingsGoal(household, "Wakacje w Japonii", "0");
  }

  @Test
  @DisplayName("usage counts only the deposits of that goal")
  void reportsUsage() throws Exception {
    var otherGoal = fixtures.savingsGoal(household, "Aparat", "0");
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "500.00", DATE, false);
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "300.00", DATE, false);
    fixtures.transaction(filip, TransactionType.SAVING, otherGoal.getId(), "999.00", DATE, false);

    mockMvc.perform(get(ITEM + "/usage").param("type", "SAVING").param("itemId", japan.getId().toString()).with(auth.as(filip)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.transactionCount").value(2))
           .andExpect(jsonPath("$.totalAmount").value(800.0));
  }

  @Test
  @DisplayName("usage of an unused goal is zero")
  void reportsZeroUsage() throws Exception {
    mockMvc.perform(get(ITEM + "/usage").param("type", "SAVING").param("itemId", japan.getId().toString()).with(auth.as(filip)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.transactionCount").value(0))
           .andExpect(jsonPath("$.totalAmount").value(0));
  }

  @Test
  @DisplayName("deleting a goal removes its deposits and keeps every other transaction")
  void deletesGoalWithDeposits() throws Exception {
    var food = fixtures.expenseCategory(household, "Jedzenie");
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "500.00", DATE, false);
    var expense = fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "40.00", DATE, false);

    mockMvc.perform(delete(ITEM).param("type", "SAVING").param("itemId", japan.getId().toString()).with(auth.as(filip)))
           .andExpect(status().isNoContent());

    assertThat(savingsGoalRepository.existsById(japan.getId())).isFalse();
    assertThat(transactionRepository.findAll()).extracting("id").containsExactly(expense.getId());
  }

  @Test
  @DisplayName("a member of another household cannot delete the goal (HTTP 404) and its deposits stay")
  void strangerCannotDelete() throws Exception {
    var stranger = fixtures.member(fixtures.household(), "Obcy", "obcy@planner.test");
    fixtures.transaction(filip, TransactionType.SAVING, japan.getId(), "500.00", DATE, false);

    mockMvc.perform(delete(ITEM).param("type", "SAVING").param("itemId", japan.getId().toString()).with(auth.as(stranger)))
           .andExpect(status().isNotFound());

    assertThat(savingsGoalRepository.existsById(japan.getId())).isTrue();
    assertThat(transactionRepository.count()).isEqualTo(1);
  }

}
