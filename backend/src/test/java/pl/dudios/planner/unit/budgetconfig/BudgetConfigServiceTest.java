package pl.dudios.planner.unit.budgetconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.model.request.ExpenseCategoryRequest;
import pl.dudios.planner.budgetconfig.model.request.SavingsGoalRequest;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link BudgetConfigService}: saving expense categories and savings goals,
 * goal ownership rules and the default configuration of a new household.
 *
 * <p>Only repositories are mocked; entities and requests are real instances.</p>
 */
@ExtendWith(MockitoExtension.class)
class BudgetConfigServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;

  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;
  @Mock
  private MemberRepository memberRepository;

  @InjectMocks
  private BudgetConfigService budgetConfigService;

  @Nested
  @DisplayName("Saving an expense category")
  class SaveExpenseCategory {

    @Test
    @DisplayName("creates a new category inside the member's household")
    void createsCategory() {
      // Given
      var request = new ExpenseCategoryRequest(null, " Zwierzęta ", "pi-heart", "#ec4899", true);

      // When
      budgetConfigService.saveExpenseCategory(HOUSEHOLD_ID, request);

      // Then
      var saved = ArgumentCaptor.forClass(ExpenseCategory.class);
      then(expenseCategoryRepository).should().save(saved.capture());
      assertThat(saved.getValue().getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
      assertThat(saved.getValue().getName()).isEqualTo("Zwierzęta");
      assertThat(saved.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("pauses an existing category without losing it")
    void pausesCategory() {
      // Given
      var category = ExpenseCategory.builder().id(3L).householdId(HOUSEHOLD_ID).name("Jedzenie").icon("pi-shopping-cart").color("#10b981").active(true).build();
      given(expenseCategoryRepository.findByIdAndHouseholdId(3L, HOUSEHOLD_ID)).willReturn(Optional.of(category));

      // When
      budgetConfigService.saveExpenseCategory(HOUSEHOLD_ID, new ExpenseCategoryRequest(3L, "Jedzenie", "pi-shopping-cart", "#10b981", false));

      // Then
      then(expenseCategoryRepository).should().save(category);
      assertThat(category.isActive()).isFalse();
    }

    @Test
    @DisplayName("cannot edit a category of another household")
    void refusesForeignCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(3L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = new ExpenseCategoryRequest(3L, "X", "pi-heart", "#ec4899", true);
      assertThatThrownBy(() -> budgetConfigService.saveExpenseCategory(HOUSEHOLD_ID, request))
        .isInstanceOf(ResourceNotFoundException.class);
      then(expenseCategoryRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Saving a savings goal")
  class SaveSavingsGoal {

    @Test
    @DisplayName("creates a shared goal when no owner is chosen")
    void createsSharedGoal() {
      // Given
      var request = goalRequest(null, null);

      // When
      budgetConfigService.saveSavingsGoal(HOUSEHOLD_ID, request);

      // Then
      var saved = savedGoal();
      assertThat(saved.getOwnerId()).isNull();
      assertThat(saved.getTargetAmount()).isEqualByComparingTo("18000");
      assertThat(saved.getInitialAmount()).isEqualByComparingTo("2000");
      then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("assigns the goal to an active member of the household")
    void assignsOwner() {
      // Given
      given(memberRepository.findByIdAndHouseholdId(2L, HOUSEHOLD_ID)).willReturn(Optional.of(member(2L, true)));

      // When
      budgetConfigService.saveSavingsGoal(HOUSEHOLD_ID, goalRequest(null, 2L));

      // Then
      assertThat(savedGoal().getOwnerId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("refuses an owner from outside the household")
    void refusesForeignOwner() {
      // Given
      given(memberRepository.findByIdAndHouseholdId(9L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = goalRequest(null, 9L);
      assertThatThrownBy(() -> budgetConfigService.saveSavingsGoal(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("nie należy");
      then(savingsGoalRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses assigning a goal to a member removed from the budget")
    void refusesRemovedOwner() {
      // Given
      given(memberRepository.findByIdAndHouseholdId(3L, HOUSEHOLD_ID)).willReturn(Optional.of(member(3L, false)));

      // When / Then
      var request = goalRequest(null, 3L);
      assertThatThrownBy(() -> budgetConfigService.saveSavingsGoal(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("usuniętej");
      then(savingsGoalRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("keeps a removed member as owner when the goal already belonged to them")
    void keepsExistingRemovedOwner() {
      // Given
      var goal = goal(4L, 3L);
      given(savingsGoalRepository.findByIdAndHouseholdId(4L, HOUSEHOLD_ID)).willReturn(Optional.of(goal));

      // When
      budgetConfigService.saveSavingsGoal(HOUSEHOLD_ID, goalRequest(4L, 3L));

      // Then
      assertThat(savedGoal().getOwnerId()).isEqualTo(3L);
      then(memberRepository).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Default configuration of a new household")
  class DefaultConfig {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("seeds active categories and an empty safety-net goal for the household")
    void seedsDefaults() {
      // Given
      ArgumentCaptor<List<ExpenseCategory>> categories = ArgumentCaptor.forClass(List.class);
      ArgumentCaptor<List<SavingsGoal>> goals = ArgumentCaptor.forClass(List.class);

      // When
      budgetConfigService.createDefaultConfig(HOUSEHOLD_ID);

      // Then
      then(expenseCategoryRepository).should().saveAll(categories.capture());
      then(savingsGoalRepository).should().saveAll(goals.capture());
      then(incomeSourceRepository).should().saveAll(any());
      assertThat(categories.getValue()).isNotEmpty().allSatisfy(category -> {
        assertThat(category.getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
        assertThat(category.isActive()).isTrue();
      });
      assertThat(goals.getValue()).isNotEmpty().allSatisfy(goal -> {
        assertThat(goal.getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
        assertThat(goal.getInitialAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(goal.getOwnerId()).isNull();
      });
    }
  }

  private SavingsGoal savedGoal() {
    var captor = ArgumentCaptor.forClass(SavingsGoal.class);
    then(savingsGoalRepository).should().save(captor.capture());
    return captor.getValue();
  }

  private SavingsGoalRequest goalRequest(Long id, Long ownerId) {
    return new SavingsGoalRequest(id, "Wakacje w Japonii", "pi-sun", "#f97316", true, "Wiosna", new BigDecimal("2000"), new BigDecimal("18000"), ownerId);
  }

  private SavingsGoal goal(Long id, Long ownerId) {
    return SavingsGoal.builder().id(id).householdId(HOUSEHOLD_ID).name("Aparat").icon("pi-camera").color("#ec4899").active(true)
                      .note("").initialAmount(BigDecimal.ZERO).targetAmount(new BigDecimal("7500")).ownerId(ownerId).build();
  }

  private Member member(Long id, boolean active) {
    return Member.builder().id(id).householdId(HOUSEHOLD_ID).firstName("Member" + id).email(id + "@planner.test").password("hash").color("#0d9488").active(active).build();
  }

}
