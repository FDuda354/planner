package pl.dudios.planner.unit.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.member.service.MemberAssignmentValidator;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
import pl.dudios.planner.transaction.repository.TransactionRepository;
import pl.dudios.planner.transaction.service.TransactionDetailsValidator;
import pl.dudios.planner.transaction.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static pl.dudios.planner.utils.AppTimeZone.EARLIEST_ON_EARTH;

/**
 * Unit tests for {@link TransactionService}: recording, editing and deleting transactions of a household.
 *
 * <p>Repositories are mocked. {@link BudgetConfigService} is a real instance backed by mocked repositories,
 * so category / source / goal lookups follow the production rules. Saved transactions are inspected through
 * an {@link ArgumentCaptor}.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long FILIP_ID = 1L;
  private static final Long KLAUDIA_ID = 2L;
  private static final Long CATEGORY_ID = 100L;
  private static final LocalDate DATE = LocalDate.of(2026, 10, 1);

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

  private TransactionService transactionService;
  private Member filip;

  @BeforeEach
  void setUp() {
    var memberAssignmentValidator = new MemberAssignmentValidator(memberRepository);
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberAssignmentValidator);
    transactionService = new TransactionService(transactionRepository, new TransactionDetailsValidator(budgetConfigService, memberAssignmentValidator));
    filip = member(FILIP_ID);
  }

  @Nested
  @DisplayName("Recording a new transaction")
  class NewTransaction {

    @Test
    @DisplayName("assigns the transaction to the chosen member and the household")
    void assignsChosenMember() {
      // Given
      givenActiveCategory();
      givenActiveFilip();

      // When
      transactionService.saveTransaction(HOUSEHOLD_ID, expense(null, true));

      // Then
      var saved = savedTransaction();
      assertThat(saved.getMemberId()).isEqualTo(FILIP_ID);
      assertThat(saved.getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
      assertThat(saved.getAmount()).isEqualByComparingTo("120.50");
      assertThat(saved.getDate()).isEqualTo(DATE);
      assertThat(saved.isShared()).isTrue();
      assertThat(saved.getNote()).isEqualTo("Biedronka");
    }

    @Test
    @DisplayName("links a saving to its goal and to no category or income source")
    void linksSavingToGoal() {
      // Given
      givenActiveFilip();
      given(savingsGoalRepository.findByIdAndHouseholdId(7L, HOUSEHOLD_ID)).willReturn(Optional.of(goal(7L)));
      var request = new TransactionRequest(null, TransactionType.SAVING, 7L, new BigDecimal("500"), DATE, FILIP_ID, false, "");

      // When
      transactionService.saveTransaction(HOUSEHOLD_ID, request);

      // Then
      var saved = savedTransaction();
      assertThat(saved.getSavingsGoalId()).isEqualTo(7L);
      assertThat(saved.getExpenseCategoryId()).isNull();
      assertThat(saved.getIncomeSourceId()).isNull();
    }

    @Test
    @DisplayName("refuses a category that does not belong to the household")
    void refusesForeignCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = expense(null, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(ResourceNotFoundException.class);
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses a date in the next month — only the current month can be planned ahead")
    void refusesNextMonth() {
      // Given
      var firstDayOfNextMonth = YearMonth.now(EARLIEST_ON_EARTH).plusMonths(1).atDay(1);
      var request = new TransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("10"), firstDayOfNextMonth, FILIP_ID, false, "");

      // When / Then
      assertThatThrownBy(() -> transactionService.saveTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("bieżącego miesiąca");
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("accepts a planned transaction on the last day of the current month")
    void acceptsLastDayOfCurrentMonth() {
      // Given
      givenActiveFilip();
      givenActiveCategory();
      var lastDayOfMonth = YearMonth.now(EARLIEST_ON_EARTH).atEndOfMonth();
      var request = new TransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("10"), lastDayOfMonth, FILIP_ID, false, "");

      // When
      transactionService.saveTransaction(HOUSEHOLD_ID, request);

      // Then
      assertThat(savedTransaction().getDate()).isEqualTo(lastDayOfMonth);
    }

    @Test
    @DisplayName("accepts today's date in the earliest time zone")
    void acceptsTodayInEarliestZone() {
      // Given
      givenActiveFilip();
      givenActiveCategory();
      var request = new TransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("10"), LocalDate.now(EARLIEST_ON_EARTH), FILIP_ID, false, "");

      // When
      transactionService.saveTransaction(HOUSEHOLD_ID, request);

      // Then
      then(transactionRepository).should().save(any(Transaction.class));
    }

    @Test
    @DisplayName("refuses to mark a saving as shared — it always goes to a goal")
    void refusesSharedSaving() {
      // Given
      given(savingsGoalRepository.findByIdAndHouseholdId(7L, HOUSEHOLD_ID)).willReturn(Optional.of(goal(7L)));
      var request = new TransactionRequest(null, TransactionType.SAVING, 7L, new BigDecimal("500"), DATE, FILIP_ID, true, "");

      // When / Then
      assertThatThrownBy(() -> transactionService.saveTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("Oszczędności");
      then(transactionRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Editing a transaction")
  class EditTransaction {

    @Test
    @DisplayName("reassigns the transaction to the member chosen in the edit")
    void reassignsChosenMember() {
      // Given
      givenActiveCategory();
      givenActiveFilip();
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.of(existingExpense(KLAUDIA_ID, CATEGORY_ID)));

      // When
      transactionService.saveTransaction(HOUSEHOLD_ID, expense(50L, false));

      // Then
      var saved = savedTransaction();
      assertThat(saved.getMemberId()).isEqualTo(FILIP_ID);
      assertThat(saved.isShared()).isFalse();
    }

    @Test
    @DisplayName("cannot edit a transaction of another household")
    void refusesForeignTransaction() {
      // Given
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = expense(50L, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(ResourceNotFoundException.class);
      then(transactionRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Deleting and listing transactions")
  class DeleteAndList {

    @Test
    @DisplayName("deletes a transaction of the member's household")
    void deletesTransaction() {
      // Given
      var transaction = existingExpense(FILIP_ID, CATEGORY_ID);
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.of(transaction));

      // When
      transactionService.deleteTransaction(HOUSEHOLD_ID, 50L);

      // Then
      then(transactionRepository).should().delete(transaction);
    }

    @Test
    @DisplayName("cannot delete a transaction of another household")
    void refusesForeignDelete() {
      // Given
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> transactionService.deleteTransaction(HOUSEHOLD_ID, 50L))
        .isInstanceOf(ResourceNotFoundException.class);
      then(transactionRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("lists the whole requested month, from the first to the last day")
    void listsWholeMonth() {
      // Given
      given(transactionRepository.findAllByHouseholdIdAndDateBetweenOrderByDateDescIdDesc(HOUSEHOLD_ID, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)))
        .willReturn(List.of(existingExpense(FILIP_ID, CATEGORY_ID)));

      // When
      var result = transactionService.getTransactions(HOUSEHOLD_ID, YearMonth.of(2024, 2));

      // Then
      assertThat(result).singleElement().satisfies(dto -> {
        assertThat(dto.itemId()).isEqualTo(CATEGORY_ID);
        assertThat(dto.memberId()).isEqualTo(FILIP_ID);
      });
    }
  }

  private void givenActiveCategory() {
    given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(category()));
  }

  private Transaction savedTransaction() {
    var captor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionRepository).should().save(captor.capture());
    return captor.getValue();
  }

  private TransactionRequest expense(Long id, boolean shared) {
    return new TransactionRequest(id, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("120.50"), DATE, FILIP_ID, shared, "Biedronka");
  }

  private Transaction existingExpense(Long authorId, Long categoryId) {
    var transaction = Transaction.builder()
                                 .id(50L)
                                 .householdId(HOUSEHOLD_ID)
                                 .amount(new BigDecimal("40"))
                                 .date(DATE)
                                 .memberId(authorId)
                                 .shared(true)
                                 .note("")
                                 .build();
    transaction.assignItem(TransactionType.EXPENSE, categoryId);
    return transaction;
  }

  private void givenActiveFilip() {
    given(memberRepository.findByIdAndHouseholdId(FILIP_ID, HOUSEHOLD_ID)).willReturn(Optional.of(filip));
  }

  private Member member(Long id) {
    return Member.builder().id(id).householdId(HOUSEHOLD_ID).firstName("Member" + id).email(id + "@planner.test").password("hash").color("#0d9488").active(true).build();
  }

  private ExpenseCategory category() {
    return ExpenseCategory.builder().id(CATEGORY_ID).householdId(HOUSEHOLD_ID).name("Jedzenie").icon("pi-shopping-cart").color("#10b981").build();
  }

  private SavingsGoal goal(Long id) {
    return SavingsGoal.builder().id(id).householdId(HOUSEHOLD_ID).name("Wakacje").icon("pi-sun").color("#f97316")
                      .note("").initialAmount(BigDecimal.ZERO).targetAmount(new BigDecimal("10000")).build();
  }

}
