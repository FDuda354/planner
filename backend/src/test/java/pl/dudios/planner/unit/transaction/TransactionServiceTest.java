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
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
import pl.dudios.planner.transaction.repository.TransactionRepository;
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
  private Member klaudia;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    transactionService = new TransactionService(transactionRepository, budgetConfigService);
    filip = member(FILIP_ID);
    klaudia = member(KLAUDIA_ID);
  }

  @Nested
  @DisplayName("Recording a new transaction")
  class NewTransaction {

    @Test
    @DisplayName("assigns the transaction to the logged-in member and their household")
    void assignsLoggedInAuthor() {
      // Given
      givenActiveCategory();

      // When
      transactionService.saveTransaction(klaudia, expense(null, true));

      // Then
      var saved = savedTransaction();
      assertThat(saved.getMemberId()).isEqualTo(KLAUDIA_ID);
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
      given(savingsGoalRepository.findByIdAndHouseholdId(7L, HOUSEHOLD_ID)).willReturn(Optional.of(goal(7L, true)));
      var request = new TransactionRequest(null, TransactionType.SAVING, 7L, new BigDecimal("500"), DATE, false, "");

      // When
      transactionService.saveTransaction(filip, request);

      // Then
      var saved = savedTransaction();
      assertThat(saved.getSavingsGoalId()).isEqualTo(7L);
      assertThat(saved.getExpenseCategoryId()).isNull();
      assertThat(saved.getIncomeSourceId()).isNull();
    }

    @Test
    @DisplayName("refuses a paused category — it has to be restored in the configuration first")
    void refusesPausedCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(category(false)));

      // When / Then
      var request = expense(null, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("Jedzenie");
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses a category that does not belong to the household")
    void refusesForeignCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = expense(null, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
        .isInstanceOf(ResourceNotFoundException.class);
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses a date that is still in the future everywhere on Earth")
    void refusesFutureDate() {
      // Given
      givenActiveCategory();
      var tomorrowEverywhere = LocalDate.now(EARLIEST_ON_EARTH).plusDays(1);
      var request = new TransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("10"), tomorrowEverywhere, false, "");

      // When / Then
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("przyszłości");
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("accepts today's date in the earliest time zone")
    void acceptsTodayInEarliestZone() {
      // Given
      givenActiveCategory();
      var request = new TransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("10"), LocalDate.now(EARLIEST_ON_EARTH), false, "");

      // When
      transactionService.saveTransaction(filip, request);

      // Then
      then(transactionRepository).should().save(any(Transaction.class));
    }

    @Test
    @DisplayName("refuses to mark an income as shared — only expenses can be shared")
    void refusesSharedIncome() {
      // Given
      given(incomeSourceRepository.findByIdAndHouseholdId(5L, HOUSEHOLD_ID)).willReturn(Optional.of(incomeSource()));
      var request = new TransactionRequest(null, TransactionType.INCOME, 5L, new BigDecimal("8000"), DATE, true, "");

      // When / Then
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("wydatek");
      then(transactionRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Editing a transaction")
  class EditTransaction {

    @Test
    @DisplayName("keeps the original author when another member edits it")
    void keepsOriginalAuthor() {
      // Given
      givenActiveCategory();
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.of(existingExpense(KLAUDIA_ID, CATEGORY_ID)));

      // When
      transactionService.saveTransaction(filip, expense(50L, false));

      // Then
      var saved = savedTransaction();
      assertThat(saved.getMemberId()).isEqualTo(KLAUDIA_ID);
      assertThat(saved.isShared()).isFalse();
    }

    @Test
    @DisplayName("allows keeping a category that was paused after the transaction was recorded")
    void keepsPausedCategoryOfEditedTransaction() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(category(false)));
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.of(existingExpense(FILIP_ID, CATEGORY_ID)));

      // When
      transactionService.saveTransaction(filip, expense(50L, false));

      // Then
      assertThat(savedTransaction().getExpenseCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("refuses moving a transaction into a paused category")
    void refusesMoveIntoPausedCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(category(false)));
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.of(existingExpense(FILIP_ID, 999L)));

      // When / Then
      var request = expense(50L, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
        .isInstanceOf(RequestValidationException.class);
      then(transactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("cannot edit a transaction of another household")
    void refusesForeignTransaction() {
      // Given
      given(transactionRepository.findByIdAndHouseholdId(50L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      var request = expense(50L, false);
      assertThatThrownBy(() -> transactionService.saveTransaction(filip, request))
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
    given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(category(true)));
  }

  private Transaction savedTransaction() {
    var captor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionRepository).should().save(captor.capture());
    return captor.getValue();
  }

  private TransactionRequest expense(Long id, boolean shared) {
    return new TransactionRequest(id, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("120.50"), DATE, shared, "Biedronka");
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

  private Member member(Long id) {
    return Member.builder().id(id).householdId(HOUSEHOLD_ID).firstName("Member" + id).email(id + "@planner.test").password("hash").color("#0d9488").active(true).build();
  }

  private ExpenseCategory category(boolean active) {
    return ExpenseCategory.builder().id(CATEGORY_ID).householdId(HOUSEHOLD_ID).name("Jedzenie").icon("pi-shopping-cart").color("#10b981").active(active).build();
  }

  private IncomeSource incomeSource() {
    return IncomeSource.builder().id(5L).householdId(HOUSEHOLD_ID).name("Wynagrodzenie").icon("pi-briefcase").color("#059669").active(true).build();
  }

  private SavingsGoal goal(Long id, boolean active) {
    return SavingsGoal.builder().id(id).householdId(HOUSEHOLD_ID).name("Wakacje").icon("pi-sun").color("#f97316").active(active)
                      .note("").initialAmount(BigDecimal.ZERO).targetAmount(new BigDecimal("10000")).build();
  }

}
