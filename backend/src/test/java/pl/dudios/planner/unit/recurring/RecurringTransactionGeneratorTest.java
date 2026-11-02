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
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.recurring.model.RecurrenceFrequency;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.recurring.service.RecurringTransactionGenerator;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link RecurringTransactionGenerator#generateDue}: turning a recurring transaction into real
 * transactions on its day, catching up missed months and skipping months it must not book.
 *
 * <p>Repositories are mocked; {@link BudgetConfigService} and the {@link RecurringTransaction} entity are real.
 * Booked transactions are inspected through an {@link ArgumentCaptor}.</p>
 */
@ExtendWith(MockitoExtension.class)
class RecurringTransactionGeneratorTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long MEMBER_ID = 2L;
  private static final Long CATEGORY_ID = 100L;

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

  private RecurringTransactionGenerator generator;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    generator = new RecurringTransactionGenerator(recurringTransactionRepository, transactionRepository, budgetConfigService, memberRepository);
  }

  @Nested
  @DisplayName("Booking a due recurring transaction")
  class Booking {

    @Test
    @DisplayName("books a transaction on its day for the chosen member and moves to the same day next month")
    void booksOnItsDay() {
      // Given
      givenBookable(true, true);
      var rent = recurring(10, LocalDate.of(2026, 11, 10));

      // When
      generator.generateDue(rent, LocalDate.of(2026, 11, 10));

      // Then
      var booked = bookedTransactions(1);
      assertThat(booked.getDate()).isEqualTo(LocalDate.of(2026, 11, 10));
      assertThat(booked.getMemberId()).isEqualTo(MEMBER_ID);
      assertThat(booked.getAmount()).isEqualByComparingTo("2500");
      assertThat(booked.getExpenseCategoryId()).isEqualTo(CATEGORY_ID);
      assertThat(booked.isShared()).isTrue();
      assertThat(booked.getRecurringTransactionId()).isEqualTo(rent.getId());
      assertThat(rent.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    @DisplayName("catches up every month missed while the application was down")
    void catchesUpMissedMonths() {
      // Given
      givenBookable(true, true);
      var rent = recurring(10, LocalDate.of(2026, 9, 10));

      // When
      generator.generateDue(rent, LocalDate.of(2026, 11, 15));

      // Then
      then(transactionRepository).should(times(3)).save(any(Transaction.class));
      assertThat(rent.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    @DisplayName("books on the last day of shorter months and returns to the 31st afterwards")
    void clampsToLastDayOfMonth() {
      // Given
      givenBookable(true, true);
      var salary = recurring(31, LocalDate.of(2026, 1, 31));

      // When
      generator.generateDue(salary, LocalDate.of(2026, 3, 31));

      // Then
      var captor = ArgumentCaptor.forClass(Transaction.class);
      then(transactionRepository).should(times(3)).save(captor.capture());
      assertThat(captor.getAllValues()).extracting(Transaction::getDate)
        .containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("does nothing before the day comes")
    void waitsForItsDay() {
      // Given
      var rent = recurring(10, LocalDate.of(2026, 11, 10));

      // When
      generator.generateDue(rent, LocalDate.of(2026, 11, 9));

      // Then
      then(transactionRepository).shouldHaveNoInteractions();
      assertThat(rent.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    @DisplayName("does nothing while the recurring transaction is paused")
    void skipsPausedRecurring() {
      // Given
      var rent = recurring(10, LocalDate.of(2026, 11, 10));
      rent.setActive(false);

      // When
      generator.generateDue(rent, LocalDate.of(2026, 11, 20));

      // Then
      then(transactionRepository).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("Months that must not be booked")
  class Skipping {

    @Test
    @DisplayName("skips the month of a member removed from the budget without booking it later")
    void skipsRemovedMember() {
      // Given
      given(memberRepository.existsByIdAndActiveTrue(MEMBER_ID)).willReturn(false);
      var salary = recurring(10, LocalDate.of(2026, 11, 10));

      // When
      generator.generateDue(salary, LocalDate.of(2026, 11, 10));

      // Then
      then(transactionRepository).should(never()).save(any());
      assertThat(salary.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    @DisplayName("skips the month while its category is paused")
    void skipsPausedCategory() {
      // Given
      givenBookable(true, false);
      var rent = recurring(10, LocalDate.of(2026, 11, 10));

      // When
      generator.generateDue(rent, LocalDate.of(2026, 11, 10));

      // Then
      then(transactionRepository).should(never()).save(any());
      assertThat(rent.getNextOccurrence()).isEqualTo(LocalDate.of(2026, 12, 10));
    }
  }

  @Nested
  @DisplayName("Booking a yearly recurring transaction")
  class YearlyBooking {

    @Test
    @DisplayName("books once on its day and moves to the same day next year")
    void booksOnceAndMovesToNextYear() {
      // Given
      givenBookable(true, true);
      var insurance = yearly(3, 15, LocalDate.of(2026, 3, 15));

      // When
      generator.generateDue(insurance, LocalDate.of(2026, 3, 15));

      // Then
      assertThat(bookedTransactions(1).getDate()).isEqualTo(LocalDate.of(2026, 3, 15));
      assertThat(insurance.getNextOccurrence()).isEqualTo(LocalDate.of(2027, 3, 15));
    }

    @Test
    @DisplayName("set for 29 February, it is booked on 28 February in a common year and on the 29th in a leap year")
    void clampsLeapDayInCommonYear() {
      // Given
      givenBookable(true, true);
      var subscription = yearly(2, 29, LocalDate.of(2027, 2, 28));

      // When
      generator.generateDue(subscription, LocalDate.of(2027, 2, 28));

      // Then
      assertThat(bookedTransactions(1).getDate()).isEqualTo(LocalDate.of(2027, 2, 28));
      assertThat(subscription.getNextOccurrence()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("books a missed yearly occurrence once, not for every month that passed")
    void booksMissedYearlyOccurrenceOnce() {
      // Given
      givenBookable(true, true);
      var insurance = yearly(3, 15, LocalDate.of(2026, 3, 15));

      // When
      generator.generateDue(insurance, LocalDate.of(2026, 11, 2));

      // Then
      then(transactionRepository).should(times(1)).save(any(Transaction.class));
      assertThat(insurance.getNextOccurrence()).isEqualTo(LocalDate.of(2027, 3, 15));
    }
  }

  private void givenBookable(boolean memberActive, boolean categoryActive) {
    given(memberRepository.existsByIdAndActiveTrue(MEMBER_ID)).willReturn(memberActive);
    given(expenseCategoryRepository.findByIdAndHouseholdId(CATEGORY_ID, HOUSEHOLD_ID)).willReturn(Optional.of(
      ExpenseCategory.builder().id(CATEGORY_ID).householdId(HOUSEHOLD_ID).name("Mieszkanie").icon("pi-home").color("#6366f1").active(categoryActive).build()));
  }

  private Transaction bookedTransactions(int expectedCount) {
    var captor = ArgumentCaptor.forClass(Transaction.class);
    then(transactionRepository).should(times(expectedCount)).save(captor.capture());
    return captor.getValue();
  }

  private RecurringTransaction yearly(int monthOfYear, int dayOfMonth, LocalDate nextOccurrence) {
    var recurring = recurring(dayOfMonth, nextOccurrence);
    recurring.setFrequency(RecurrenceFrequency.YEARLY);
    recurring.setMonthOfYear(monthOfYear);
    return recurring;
  }

  private RecurringTransaction recurring(int dayOfMonth, LocalDate nextOccurrence) {
    var recurring = RecurringTransaction.builder()
                                        .id(5L)
                                        .householdId(HOUSEHOLD_ID)
                                        .amount(new BigDecimal("2500"))
                                        .dayOfMonth(dayOfMonth)
                                        .memberId(MEMBER_ID)
                                        .shared(true)
                                        .note("Czynsz")
                                        .active(true)
                                        .nextOccurrence(nextOccurrence)
                                        .build();
    recurring.assignItem(TransactionType.EXPENSE, CATEGORY_ID);
    return recurring;
  }

}
