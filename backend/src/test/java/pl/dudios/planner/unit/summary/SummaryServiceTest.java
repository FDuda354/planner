package pl.dudios.planner.unit.summary;

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
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.summary.model.MonthlyAmount;
import pl.dudios.planner.summary.model.dto.ItemAmountDTO;
import pl.dudios.planner.summary.model.dto.MemberAmountDTO;
import pl.dudios.planner.summary.model.dto.MonthSummaryDTO;
import pl.dudios.planner.summary.model.dto.MonthlyContributionDTO;
import pl.dudios.planner.summary.repository.SummaryRepository;
import pl.dudios.planner.summary.service.SummaryService;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link SummaryService}: monthly and yearly totals, the "who spent how much" split
 * (shared expenses excluded) and progress of savings goals.
 *
 * <p>{@link SummaryRepository} and the budget configuration repositories are mocked and return
 * pre-aggregated {@link MonthlyAmount} rows, exactly like the database aggregation does.</p>
 */
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long FILIP_ID = 1L;
  private static final Long KLAUDIA_ID = 2L;
  private static final Long FOOD_ID = 100L;
  private static final Long FUN_ID = 101L;
  private static final Long SALARY_ID = 200L;
  private static final Long JAPAN_ID = 300L;

  @Mock
  private SummaryRepository summaryRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;
  @Mock
  private MemberRepository memberRepository;

  private SummaryService summaryService;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    summaryService = new SummaryService(summaryRepository, budgetConfigService);
  }

  @Nested
  @DisplayName("Month summaries")
  class MonthSummaries {

    @Test
    @DisplayName("lists months from the newest to the oldest")
    void ordersNewestFirst() {
      // Given
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        expense(2026, 8, FOOD_ID, FILIP_ID, false, "100"),
        expense(2026, 10, FOOD_ID, FILIP_ID, false, "100"),
        expense(2026, 9, FOOD_ID, FILIP_ID, false, "100")
      ));

      // When
      var result = summaryService.getMonthSummaries(HOUSEHOLD_ID);

      // Then
      assertThat(result).extracting(MonthSummaryDTO::month).containsExactly("2026-10", "2026-09", "2026-08");
    }

    @Test
    @DisplayName("totals incomes, expenses and savings of a month separately")
    void totalsPerType() {
      // Given
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        income(2026, 10, SALARY_ID, FILIP_ID, "8000"),
        expense(2026, 10, FOOD_ID, FILIP_ID, true, "600"),
        expense(2026, 10, FUN_ID, KLAUDIA_ID, false, "150"),
        saving(2026, 10, JAPAN_ID, FILIP_ID, "1000")
      ));

      // When
      var month = summaryService.getMonthSummaries(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(month.income()).isEqualByComparingTo("8000");
      assertThat(month.expenses()).isEqualByComparingTo("750");
      assertThat(month.savings()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("counts only personal expenses in \"who spent how much\" and reports shared ones separately")
    void excludesSharedExpensesFromPersonalSplit() {
      // Given
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        expense(2026, 10, FOOD_ID, FILIP_ID, true, "600"),
        expense(2026, 10, FUN_ID, FILIP_ID, false, "90"),
        expense(2026, 10, FUN_ID, KLAUDIA_ID, false, "150"),
        expense(2026, 10, FOOD_ID, KLAUDIA_ID, true, "400")
      ));

      // When
      var month = summaryService.getMonthSummaries(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(month.personalExpensesByMember())
        .extracting(MemberAmountDTO::memberId, dto -> dto.amount().toPlainString())
        .containsExactly(tuple(KLAUDIA_ID, "150"), tuple(FILIP_ID, "90"));
      assertThat(month.sharedExpenses()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("ranks categories by the amount spent, the biggest first")
    void ranksCategories() {
      // Given
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        expense(2026, 10, FUN_ID, FILIP_ID, false, "150"),
        expense(2026, 10, FOOD_ID, FILIP_ID, true, "300"),
        expense(2026, 10, FOOD_ID, KLAUDIA_ID, true, "400")
      ));

      // When
      var month = summaryService.getMonthSummaries(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(month.expensesByCategory())
        .extracting(ItemAmountDTO::itemId, dto -> dto.amount().toPlainString())
        .containsExactly(tuple(FOOD_ID, "700"), tuple(FUN_ID, "150"));
    }
  }

  @Nested
  @DisplayName("Year summary")
  class YearSummary {

    @Test
    @DisplayName("always returns all twelve months, with zeros for months without transactions")
    void returnsTwelveMonths() {
      // Given
      given(summaryRepository.findMonthlyAmountsBetween(HOUSEHOLD_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))
        .willReturn(List.of(income(2026, 3, SALARY_ID, FILIP_ID, "8000")));

      // When
      var result = summaryService.getYearSummary(HOUSEHOLD_ID, 2026);

      // Then
      assertThat(result).hasSize(12);
      assertThat(result.getFirst().month()).isEqualTo("2026-01");
      assertThat(result.getLast().month()).isEqualTo("2026-12");
      assertThat(result.get(2).income()).isEqualByComparingTo("8000");
      assertThat(result.get(3).income()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.get(3).expensesByCategory()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Goal progress")
  class GoalProgress {

    @Test
    @DisplayName("adds deposits of the goal to the amount saved at the start, month by month")
    void sumsDepositsOnTopOfInitialAmount() {
      // Given
      given(savingsGoalRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(goal(JAPAN_ID, "2000")));
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        saving(2026, 9, JAPAN_ID, FILIP_ID, "500"),
        saving(2026, 9, JAPAN_ID, KLAUDIA_ID, "300"),
        saving(2026, 10, JAPAN_ID, FILIP_ID, "1000")
      ));

      // When
      var progress = summaryService.getGoalProgress(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(progress.saved()).isEqualByComparingTo("3800");
      assertThat(progress.contributions())
        .extracting(MonthlyContributionDTO::month, dto -> dto.amount().toPlainString())
        .containsExactly(tuple("2026-09", "800"), tuple("2026-10", "1000"));
    }

    @Test
    @DisplayName("ignores deposits made to other goals and amounts of other transaction types")
    void ignoresOtherGoalsAndTypes() {
      // Given
      given(savingsGoalRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(goal(JAPAN_ID, "0")));
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of(
        saving(2026, 10, 999L, FILIP_ID, "700"),
        expense(2026, 10, JAPAN_ID, FILIP_ID, false, "50"),
        saving(2026, 10, JAPAN_ID, FILIP_ID, "100")
      ));

      // When
      var progress = summaryService.getGoalProgress(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(progress.saved()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("reports a goal without deposits with only its starting amount")
    void reportsGoalWithoutDeposits() {
      // Given
      given(savingsGoalRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(goal(JAPAN_ID, "2000")));
      given(summaryRepository.findMonthlyAmounts(HOUSEHOLD_ID)).willReturn(List.of());

      // When
      var progress = summaryService.getGoalProgress(HOUSEHOLD_ID).getFirst();

      // Then
      assertThat(progress.saved()).isEqualByComparingTo("2000");
      assertThat(progress.contributions()).isEmpty();
    }
  }

  private MonthlyAmount expense(int year, int month, Long itemId, Long memberId, boolean shared, String amount) {
    return new MonthlyAmount(year, month, TransactionType.EXPENSE, itemId, memberId, shared, new BigDecimal(amount));
  }

  private MonthlyAmount income(int year, int month, Long itemId, Long memberId, String amount) {
    return new MonthlyAmount(year, month, TransactionType.INCOME, itemId, memberId, false, new BigDecimal(amount));
  }

  private MonthlyAmount saving(int year, int month, Long itemId, Long memberId, String amount) {
    return new MonthlyAmount(year, month, TransactionType.SAVING, itemId, memberId, false, new BigDecimal(amount));
  }

  private SavingsGoal goal(Long id, String initialAmount) {
    return SavingsGoal.builder().id(id).householdId(HOUSEHOLD_ID).name("Wakacje w Japonii").icon("pi-sun").color("#f97316").active(true)
                      .note("").initialAmount(new BigDecimal(initialAmount)).targetAmount(new BigDecimal("18000")).build();
  }

}
