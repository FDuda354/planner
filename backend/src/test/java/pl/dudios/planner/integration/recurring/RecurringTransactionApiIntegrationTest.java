package pl.dudios.planner.integration.recurring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.recurring.model.RecurringTransaction;
import pl.dudios.planner.recurring.repository.RecurringTransactionRepository;
import pl.dudios.planner.recurring.service.RecurringTransactionGenerator;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.dudios.planner.utils.AppTimeZone.POLAND;

/**
 * Integration tests of recurring transactions ({@code /api/recurring-transaction}) and of the generator that
 * books them, against real PostgreSQL with its foreign keys and cascades.
 *
 * <p>Assertions read the HTTP response or a fresh state from the repositories.</p>
 */
class RecurringTransactionApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ROOT = "/api/recurring-transaction";

  @Autowired
  private RecurringTransactionRepository recurringTransactionRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private RecurringTransactionGenerator generator;

  private Household household;
  private Member filip;
  private Member klaudia;
  private IncomeSource salary;
  private ExpenseCategory housing;

  @BeforeEach
  void setUpHousehold() {
    household = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
    salary = fixtures.incomeSource(household, "Wynagrodzenie");
    housing = fixtures.expenseCategory(household, "Mieszkanie", true);
  }

  @Nested
  @DisplayName("Setting up")
  class SettingUp {

    @Test
    @DisplayName("a salary due today is booked at once for the chosen member and shows up as recurring")
    void booksTodaysSalary() throws Exception {
      var today = LocalDate.now(POLAND);

      save(filip, salaryJson(null, today.getDayOfMonth())).andExpect(status().isNoContent());

      mockMvc.perform(get("/api/transaction").param("month", YearMonth.from(today).toString()).with(auth.as(filip)))
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].memberId").value(klaudia.getId()))
             .andExpect(jsonPath("$[0].recurring").value(true))
             .andExpect(jsonPath("$[0].date").value(today.toString()));
    }

    @Test
    @DisplayName("the list shows the household's recurring transactions with their next date")
    void listsRecurringTransactions() throws Exception {
      save(filip, salaryJson(null, 10)).andExpect(status().isNoContent());

      mockMvc.perform(get(ROOT).with(auth.as(klaudia)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(1)))
             .andExpect(jsonPath("$[0].dayOfMonth").value(10))
             .andExpect(jsonPath("$[0].nextOccurrence").isNotEmpty());
    }

    @Test
    @DisplayName("a yearly transaction is stored with its month and first scheduled for that day")
    void storesYearlySchedule() throws Exception {
      var tomorrow = LocalDate.now(POLAND).plusDays(1);
      var body = """
        {"type": "INCOME", "itemId": %d, "amount": 1200, "frequency": "YEARLY", "dayOfMonth": %d, "monthOfYear": %d, "memberId": %d, "shared": false, "note": "Premia", "active": true}
        """.formatted(salary.getId(), tomorrow.getDayOfMonth(), tomorrow.getMonthValue(), klaudia.getId());

      save(filip, body).andExpect(status().isNoContent());

      mockMvc.perform(get(ROOT).with(auth.as(filip)))
             .andExpect(jsonPath("$[0].frequency").value("YEARLY"))
             .andExpect(jsonPath("$[0].monthOfYear").value(tomorrow.getMonthValue()))
             .andExpect(jsonPath("$[0].nextOccurrence").value(tomorrow.toString()));
    }

    @Test
    @DisplayName("refuses a day of month outside 1–31 with HTTP 400")
    void refusesInvalidDay() throws Exception {
      save(filip, salaryJson(null, 32)).andExpect(status().isBadRequest());

      assertThat(recurringTransactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("another household cannot see them")
    void isolatesHouseholds() throws Exception {
      save(filip, salaryJson(null, 10)).andExpect(status().isNoContent());
      var stranger = fixtures.member(fixtures.household(), "Obcy", "obcy@planner.test");

      mockMvc.perform(get(ROOT).with(auth.as(stranger)))
             .andExpect(jsonPath("$", hasSize(0)));
    }
  }

  @Nested
  @DisplayName("Daily generation")
  class Generation {

    @Test
    @DisplayName("books every missed month up to today and only once per month")
    void catchesUpOnce() {
      var twoMonthsAgo = YearMonth.now(POLAND).minusMonths(2).atDay(1);
      recurringRent(twoMonthsAgo);

      generator.generateDueTransactions();
      generator.generateDueTransactions();

      assertThat(transactionRepository.findAll()).extracting(Transaction::getDate)
        .containsExactlyInAnyOrder(twoMonthsAgo, twoMonthsAgo.plusMonths(1), twoMonthsAgo.plusMonths(2));
    }
  }

  @Nested
  @DisplayName("Removing")
  class Removing {

    @Test
    @DisplayName("deleting a recurring transaction keeps the transactions it already booked")
    void keepsBookedTransactions() throws Exception {
      var rent = recurringRent(YearMonth.now(POLAND).atDay(1));
      generator.generateDueTransactions();

      mockMvc.perform(delete(ROOT).param("recurringTransactionId", rent.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNoContent());

      assertThat(recurringTransactionRepository.count()).isZero();
      assertThat(transactionRepository.findAll()).singleElement()
                                                 .satisfies(transaction -> assertThat(transaction.getRecurringTransactionId()).isNull());
    }

    @Test
    @DisplayName("deleting its category removes the recurring transaction too")
    void removedWithCategory() throws Exception {
      recurringRent(YearMonth.now(POLAND).plusMonths(1).atDay(1));

      mockMvc.perform(delete("/api/budget-config/item").param("type", "EXPENSE").param("itemId", housing.getId().toString())
                                                       .with(auth.as(filip)))
             .andExpect(status().isNoContent());

      assertThat(recurringTransactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("a member of another household cannot delete it (HTTP 404)")
    void strangerCannotDelete() throws Exception {
      var rent = recurringRent(YearMonth.now(POLAND).plusMonths(1).atDay(1));
      var stranger = fixtures.member(fixtures.household(), "Obcy", "obcy@planner.test");

      mockMvc.perform(delete(ROOT).param("recurringTransactionId", rent.getId().toString()).with(auth.as(stranger)))
             .andExpect(status().isNotFound());

      assertThat(recurringTransactionRepository.existsById(rent.getId())).isTrue();
    }
  }

  private ResultActions save(Member author, String body) throws Exception {
    return mockMvc.perform(post(ROOT).with(auth.as(author)).contentType(APPLICATION_JSON).content(body));
  }

  private String salaryJson(Long id, int dayOfMonth) {
    return """
      {"id": %s, "type": "INCOME", "itemId": %d, "amount": 8000, "frequency": "MONTHLY", "dayOfMonth": %d, "memberId": %d, "shared": false, "note": "Pensja", "active": true}
      """.formatted(id, salary.getId(), dayOfMonth, klaudia.getId());
  }

  private RecurringTransaction recurringRent(LocalDate nextOccurrence) {
    var rent = RecurringTransaction.builder()
                                   .householdId(household.getId())
                                   .amount(new BigDecimal("2500"))
                                   .dayOfMonth(1)
                                   .memberId(filip.getId())
                                   .shared(true)
                                   .note("Czynsz")
                                   .active(true)
                                   .nextOccurrence(nextOccurrence)
                                   .build();
    rent.assignItem(TransactionType.EXPENSE, housing.getId());
    return recurringTransactionRepository.save(rent);
  }

}
