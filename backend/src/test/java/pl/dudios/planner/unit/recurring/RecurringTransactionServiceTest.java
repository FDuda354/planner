package pl.dudios.planner.unit.recurring;

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
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.member.service.MemberAssignmentValidator;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.model.request.RecurringTransactionRequest;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.recurring.service.RecurringTransactionGenerator;
import pl.dudios.planner.recurring.service.RecurringTransactionService;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static pl.dudios.planner.utils.AppTimeZone.POLAND;

/**
 * Unit tests for {@link RecurringTransactionService}: setting up, changing and deleting recurring transactions
 * of a household.
 *
 * <p>Repositories are mocked. {@link RecurringTransactionGenerator} and {@link BudgetConfigService} are real
 * instances, so saving a recurring transaction due today observably books it.</p>
 */
@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long KLAUDIA_ID = 2L;
  private static final Long CATEGORY_ID = 100L;
  private static final Long SALARY_ID = 200L;

  @Mock
  private RecurringTransactionRepository recurringTransactionRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private MemberRepository memberRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;

  private RecurringTransactionService service;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    var generator = new RecurringTransactionGenerator(recurringTransactionRepository, transactionRepository, budgetConfigService, memberRepository);
    service = new RecurringTransactionService(recurringTransactionRepository, generator, budgetConfigService, new MemberAssignmentValidator(memberRepository));
  }

  @Nested
  @DisplayName("Setting up a recurring transaction")
  class Create {

    @Test
    @DisplayName("set up for today's day of month, it is booked right away and next scheduled for next month")
    void booksTodaysOccurrenceImmediately() {
      // Given
      var today = LocalDate.now(POLAND);
      givenActiveSalarySource();
      givenKlaudia(true);
      givenSavedRecurringReturned();
      given(memberRepository.existsByIdAndActiveTrue(KLAUDIA_ID)).willReturn(true);

      // When
      service.saveRecurringTransaction(HOUSEHOLD_ID, salary(null, today.getDayOfMonth()));

      // Then
      var booked = ArgumentCaptor.forClass(Transaction.class);
      then(transactionRepository).should().save(booked.capture());
      assertThat(booked.getValue().getDate()).isEqualTo(today);
      assertThat(booked.getValue().getMemberId()).isEqualTo(KLAUDIA_ID);
      assertThat(savedRecurring().getNextOccurrence()).isAfter(today);
    }

    @Test
    @DisplayName("belongs to the chosen member, not to whoever set it up")
    void assignsChosenMember() {
      // Given
      givenActiveSalarySource();
      givenKlaudia(true);
      givenSavedRecurringReturned();
      var request = salary(null, 10);

      // When
      service.saveRecurringTransaction(HOUSEHOLD_ID, request);

      // Then
      assertThat(savedRecurring().getMemberId()).isEqualTo(KLAUDIA_ID);
      assertThat(savedRecurring().getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
    }

    @Test
    @DisplayName("refuses a member removed from the budget")
    void refusesRemovedMember() {
      // Given
      givenActiveSalarySource();
      givenKlaudia(false);
      var request = salary(null, 10);

      // When / Then
      assertThatThrownBy(() -> service.saveRecurringTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("usuniętej");
      then(recurringTransactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses a person from outside the household")
    void refusesForeignMember() {
      // Given
      givenActiveSalarySource();
      given(memberRepository.findByIdAndHouseholdId(KLAUDIA_ID, HOUSEHOLD_ID)).willReturn(Optional.empty());
      var request = salary(null, 10);

      // When / Then
      assertThatThrownBy(() -> service.saveRecurringTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("nie należy");
      then(recurringTransactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses a paused category")
    void refusesPausedCategory() {
      // Given
      given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(
        ExpenseCategory.builder().id(CATEGORY_ID).householdId(HOUSEHOLD_ID).name("Mieszkanie").icon("pi-home").color("#6366f1").active(false).build()));
      var request = new RecurringTransactionRequest(null, TransactionType.EXPENSE, CATEGORY_ID, new BigDecimal("2500"), 10, KLAUDIA_ID, true, "Czynsz", true);

      // When / Then
      assertThatThrownBy(() -> service.saveRecurringTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("Mieszkanie");
      then(recurringTransactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses to mark an income as shared")
    void refusesSharedIncome() {
      // Given
      givenActiveSalarySource();
      givenKlaudia(true);
      var request = new RecurringTransactionRequest(null, TransactionType.INCOME, SALARY_ID, new BigDecimal("8000"), 10, KLAUDIA_ID, true, "", true);

      // When / Then
      assertThatThrownBy(() -> service.saveRecurringTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("wydatek");
      then(recurringTransactionRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Changing and deleting")
  class ChangeAndDelete {

    @Test
    @DisplayName("changing only the amount keeps the planned next date")
    void keepsScheduleWhenDayUnchanged() {
      // Given
      var plannedDate = YearMonth.now(POLAND).plusMonths(1).atDay(10);
      var existing = existingSalary(10, plannedDate);
      given(recurringTransactionRepository.findByIdAndHouseholdId(5L, HOUSEHOLD_ID)).willReturn(Optional.of(existing));
      givenActiveSalarySource();
      givenKlaudia(true);
      givenSavedRecurringReturned();

      // When
      service.saveRecurringTransaction(HOUSEHOLD_ID, new RecurringTransactionRequest(5L, TransactionType.INCOME, SALARY_ID, new BigDecimal("9000"), 10, KLAUDIA_ID, false, "", true));

      // Then
      assertThat(existing.getNextOccurrence()).isEqualTo(plannedDate);
      assertThat(existing.getAmount()).isEqualByComparingTo("9000");
    }

    @Test
    @DisplayName("cannot change a recurring transaction of another household")
    void refusesForeignRecurring() {
      // Given
      given(recurringTransactionRepository.findByIdAndHouseholdId(5L, HOUSEHOLD_ID)).willReturn(Optional.empty());
      var request = salary(5L, 10);

      // When / Then
      assertThatThrownBy(() -> service.saveRecurringTransaction(HOUSEHOLD_ID, request))
        .isInstanceOf(ResourceNotFoundException.class);
      then(recurringTransactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("deletes a recurring transaction of the household")
    void deletesRecurring() {
      // Given
      var existing = existingSalary(10, LocalDate.now(POLAND).plusMonths(1));
      given(recurringTransactionRepository.findByIdAndHouseholdId(5L, HOUSEHOLD_ID)).willReturn(Optional.of(existing));

      // When
      service.deleteRecurringTransaction(HOUSEHOLD_ID, 5L);

      // Then
      then(recurringTransactionRepository).should().delete(existing);
    }

    @Test
    @DisplayName("cannot delete a recurring transaction of another household")
    void refusesForeignDelete() {
      // Given
      given(recurringTransactionRepository.findByIdAndHouseholdId(5L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> service.deleteRecurringTransaction(HOUSEHOLD_ID, 5L))
        .isInstanceOf(ResourceNotFoundException.class);
      then(recurringTransactionRepository).should(never()).delete(any());
    }
  }

  private void givenActiveSalarySource() {
    given(incomeSourceRepository.findByIdAndHouseholdId(SALARY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(
      IncomeSource.builder().id(SALARY_ID).householdId(HOUSEHOLD_ID).name("Wynagrodzenie").icon("pi-briefcase").color("#059669").active(true).build()));
  }

  private void givenKlaudia(boolean active) {
    given(memberRepository.findByIdAndHouseholdId(KLAUDIA_ID, HOUSEHOLD_ID)).willReturn(Optional.of(
      Member.builder().id(KLAUDIA_ID).householdId(HOUSEHOLD_ID).firstName("Klaudia").email("klaudia@planner.test").password("hash").color("#c026d3").active(active).build()));
  }

  private void givenSavedRecurringReturned() {
    given(recurringTransactionRepository.save(any(RecurringTransaction.class))).willAnswer(invocation -> invocation.getArgument(0));
  }

  private RecurringTransaction savedRecurring() {
    var captor = ArgumentCaptor.forClass(RecurringTransaction.class);
    then(recurringTransactionRepository).should().save(captor.capture());
    return captor.getValue();
  }

  private RecurringTransactionRequest salary(Long id, int dayOfMonth) {
    return new RecurringTransactionRequest(id, TransactionType.INCOME, SALARY_ID, new BigDecimal("8000"), dayOfMonth, KLAUDIA_ID, false, "Pensja", true);
  }

  private RecurringTransaction existingSalary(int dayOfMonth, LocalDate nextOccurrence) {
    var recurring = RecurringTransaction.builder()
                                        .id(5L)
                                        .householdId(HOUSEHOLD_ID)
                                        .amount(new BigDecimal("8000"))
                                        .dayOfMonth(dayOfMonth)
                                        .memberId(KLAUDIA_ID)
                                        .note("")
                                        .active(true)
                                        .nextOccurrence(nextOccurrence)
                                        .build();
    recurring.assignItem(TransactionType.INCOME, SALARY_ID);
    return recurring;
  }

}
