package pl.dudios.planner.integration.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
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
import static pl.dudios.planner.utils.AppTimeZone.EARLIEST_ON_EARTH;

/**
 * Integration tests of {@code /api/transaction}: who a transaction belongs to, the validation rules and
 * the isolation between households.
 *
 * <p>Assertions read the HTTP response or a fresh state from {@link TransactionRepository}; no internals
 * are inspected.</p>
 */
class TransactionApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ROOT = "/api/transaction";
  private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

  @Autowired
  private TransactionRepository transactionRepository;

  private Household household;
  private Household strangerHousehold;
  private Member filip;
  private Member klaudia;
  private Member stranger;
  private ExpenseCategory food;

  @BeforeEach
  void setUpHouseholds() {
    household = fixtures.household();
    strangerHousehold = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
    food = fixtures.expenseCategory(household, "Jedzenie", true);
    stranger = fixtures.member(strangerHousehold, "Obcy", "obcy@planner.test");
  }

  @Nested
  @DisplayName("Recording a transaction")
  class Recording {

    @Test
    @DisplayName("assigns the transaction to the logged-in member")
    void assignsLoggedInMember() throws Exception {
      mockMvc.perform(post(ROOT).with(auth.as(klaudia)).contentType(APPLICATION_JSON)
                                .content(json(expense(null, "120.50", DATE, true))))
             .andExpect(status().isNoContent());

      var saved = transactionRepository.findAll().getFirst();
      assertThat(saved.getMemberId()).isEqualTo(klaudia.getId());
      assertThat(saved.getHouseholdId()).isEqualTo(klaudia.getHouseholdId());
      assertThat(saved.isShared()).isTrue();
    }

    @Test
    @DisplayName("ignores a payer smuggled into the request body")
    void ignoresPayerFromBody() throws Exception {
      var body = """
        {"type": "EXPENSE", "itemId": %d, "amount": 10, "date": "%s", "memberId": %d, "shared": false, "note": ""}
        """.formatted(food.getId(), DATE, filip.getId());

      mockMvc.perform(post(ROOT).with(auth.as(klaudia)).contentType(APPLICATION_JSON).content(body))
             .andExpect(status().isNoContent());

      assertThat(transactionRepository.findAll().getFirst().getMemberId()).isEqualTo(klaudia.getId());
    }

    @Test
    @DisplayName("refuses a date in the future with HTTP 400")
    void refusesFutureDate() throws Exception {
      var tomorrowEverywhere = LocalDate.now(EARLIEST_ON_EARTH).plusDays(1);

      mockMvc.perform(post(ROOT).with(auth.as(filip)).contentType(APPLICATION_JSON)
                                .content(json(expense(null, "10", tomorrowEverywhere, false))))
             .andExpect(status().isBadRequest());

      assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses a zero amount with HTTP 400")
    void refusesZeroAmount() throws Exception {
      mockMvc.perform(post(ROOT).with(auth.as(filip)).contentType(APPLICATION_JSON)
                                .content(json(expense(null, "0", DATE, false))))
             .andExpect(status().isBadRequest());

      assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses a paused category with HTTP 400")
    void refusesPausedCategory() throws Exception {
      var paused = fixtures.expenseCategory(household, "Stare", false);
      var request = new TransactionRequest(null, TransactionType.EXPENSE, paused.getId(), new BigDecimal("10"), DATE, false, "");

      mockMvc.perform(post(ROOT).with(auth.as(filip)).contentType(APPLICATION_JSON).content(json(request)))
             .andExpect(status().isBadRequest());

      assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses a shared income with HTTP 400")
    void refusesSharedIncome() throws Exception {
      var salary = fixtures.incomeSource(household, "Wynagrodzenie");
      var request = new TransactionRequest(null, TransactionType.INCOME, salary.getId(), new BigDecimal("8000"), DATE, true, "");

      mockMvc.perform(post(ROOT).with(auth.as(filip)).contentType(APPLICATION_JSON).content(json(request)))
             .andExpect(status().isBadRequest());

      assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses a category of another household with HTTP 404")
    void refusesForeignCategory() throws Exception {
      mockMvc.perform(post(ROOT).with(auth.as(stranger)).contentType(APPLICATION_JSON)
                                .content(json(expense(null, "10", DATE, false))))
             .andExpect(status().isNotFound());

      assertThat(transactionRepository.count()).isZero();
    }
  }

  @Nested
  @DisplayName("Editing and deleting")
  class EditingAndDeleting {

    @Test
    @DisplayName("editing by the partner keeps the original author")
    void editKeepsAuthor() throws Exception {
      var transaction = fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "40", DATE, true);

      mockMvc.perform(post(ROOT).with(auth.as(filip)).contentType(APPLICATION_JSON)
                                .content(json(expense(transaction.getId(), "55", DATE, false))))
             .andExpect(status().isNoContent());

      var edited = reload(transaction);
      assertThat(edited.getMemberId()).isEqualTo(klaudia.getId());
      assertThat(edited.getAmount()).isEqualByComparingTo("55");
      assertThat(edited.isShared()).isFalse();
    }

    @Test
    @DisplayName("a member of another household cannot edit the transaction")
    void strangerCannotEdit() throws Exception {
      var transaction = fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "40", DATE, true);

      mockMvc.perform(post(ROOT).with(auth.as(stranger)).contentType(APPLICATION_JSON)
                                .content(json(expense(transaction.getId(), "1", DATE, false))))
             .andExpect(status().isNotFound());

      assertThat(reload(transaction).getAmount()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("deletes a transaction of the household")
    void deletesTransaction() throws Exception {
      var transaction = fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "40", DATE, true);

      mockMvc.perform(delete(ROOT).param("transactionId", transaction.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNoContent());

      assertThat(transactionRepository.existsById(transaction.getId())).isFalse();
    }

    @Test
    @DisplayName("a member of another household cannot delete the transaction")
    void strangerCannotDelete() throws Exception {
      var transaction = fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "40", DATE, true);

      mockMvc.perform(delete(ROOT).param("transactionId", transaction.getId().toString()).with(auth.as(stranger)))
             .andExpect(status().isNotFound());

      assertThat(transactionRepository.existsById(transaction.getId())).isTrue();
    }
  }

  @Nested
  @DisplayName("Listing a month")
  class ListingMonth {

    @Test
    @DisplayName("returns only the requested month of the member's own household, newest first")
    void listsOwnMonth() throws Exception {
      fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "10", LocalDate.of(2026, 9, 1), false);
      fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "20", LocalDate.of(2026, 9, 30), true);
      fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "30", LocalDate.of(2026, 10, 1), false);
      var strangerCategory = fixtures.expenseCategory(strangerHousehold, "Obce", true);
      fixtures.transaction(stranger, TransactionType.EXPENSE, strangerCategory.getId(), "99", LocalDate.of(2026, 9, 10), false);

      mockMvc.perform(get(ROOT).param("month", YearMonth.of(2026, 9).toString()).with(auth.as(filip)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$", hasSize(2)))
             .andExpect(jsonPath("$[0].date").value("2026-09-30"))
             .andExpect(jsonPath("$[0].memberId").value(klaudia.getId()))
             .andExpect(jsonPath("$[1].date").value("2026-09-01"));
    }
  }

  private TransactionRequest expense(Long id, String amount, LocalDate date, boolean shared) {
    return new TransactionRequest(id, TransactionType.EXPENSE, food.getId(), new BigDecimal(amount), date, shared, "");
  }

  private Transaction reload(Transaction transaction) {
    return transactionRepository.findById(transaction.getId()).orElseThrow();
  }

  private String json(Object body) {
    return objectMapper.writeValueAsString(body);
  }

}
