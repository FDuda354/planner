package pl.dudios.planner.unit.budgetconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.budgetconfig.service.BudgetItemRemovalService;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.member.service.MemberAssignmentValidator;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.dto.ItemUsageDTO;
import pl.dudios.planner.transaction.repository.TransactionRepository;
import pl.dudios.planner.transaction.service.TransactionDetailsValidator;
import pl.dudios.planner.transaction.service.TransactionService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link BudgetItemRemovalService}: previewing what a deletion takes with it and deleting
 * a budget item together with its transactions.
 *
 * <p>Repositories are mocked; {@link BudgetConfigService} and {@link TransactionService} are real instances.</p>
 */
@ExtendWith(MockitoExtension.class)
class BudgetItemRemovalServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long GOAL_ID = 7L;

  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;
  @Mock
  private MemberRepository memberRepository;

  private BudgetItemRemovalService removalService;

  @BeforeEach
  void setUp() {
    var memberAssignmentValidator = new MemberAssignmentValidator(memberRepository);
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberAssignmentValidator);
    var transactionService = new TransactionService(transactionRepository, new TransactionDetailsValidator(budgetConfigService, memberAssignmentValidator));
    removalService = new BudgetItemRemovalService(budgetConfigService, transactionService);
  }

  @Nested
  @DisplayName("Previewing a deletion")
  class Usage {

    @Test
    @DisplayName("reports how many transactions and how much money go away with the item")
    void reportsUsage() {
      // Given
      given(savingsGoalRepository.findByIdAndHouseholdId(GOAL_ID, HOUSEHOLD_ID)).willReturn(Optional.of(goal()));
      given(transactionRepository.findItemUsage(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID))
        .willReturn(new ItemUsageDTO(3L, new BigDecimal("1500")));

      // When
      var usage = removalService.getItemUsage(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID);

      // Then
      assertThat(usage.transactionCount()).isEqualTo(3L);
      assertThat(usage.totalAmount()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("refuses to describe an item of another household")
    void refusesForeignItem() {
      // Given
      given(savingsGoalRepository.findByIdAndHouseholdId(GOAL_ID, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> removalService.getItemUsage(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID))
        .isInstanceOf(ResourceNotFoundException.class);
      then(transactionRepository).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Deleting an item")
  class Delete {

    @Test
    @DisplayName("deletes the goal together with all its deposits")
    void deletesGoalWithTransactions() {
      // Given
      var goal = goal();
      given(savingsGoalRepository.findByIdAndHouseholdId(GOAL_ID, HOUSEHOLD_ID)).willReturn(Optional.of(goal));

      // When
      removalService.deleteItemWithTransactions(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID);

      // Then
      then(transactionRepository).should().deleteAllByItem(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID);
      then(savingsGoalRepository).should().delete(goal);
    }

    @Test
    @DisplayName("fails for an item of another household without deleting any item")
    void refusesForeignItem() {
      // Given
      given(savingsGoalRepository.findByIdAndHouseholdId(GOAL_ID, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> removalService.deleteItemWithTransactions(HOUSEHOLD_ID, TransactionType.SAVING, GOAL_ID))
        .isInstanceOf(ResourceNotFoundException.class);
      then(savingsGoalRepository).should(never()).delete(any());
    }
  }

  private SavingsGoal goal() {
    return SavingsGoal.builder().id(GOAL_ID).householdId(HOUSEHOLD_ID).name("Wakacje").icon("pi-sun").color("#f97316")
                      .note("").initialAmount(BigDecimal.ZERO).targetAmount(new BigDecimal("10000")).build();
  }

}
