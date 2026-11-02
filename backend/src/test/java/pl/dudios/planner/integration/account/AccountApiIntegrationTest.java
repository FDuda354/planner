package pl.dudios.planner.integration.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.integration.fixtures.TestFixtures;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.RegisterRequest;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@code /api/account}: deleting one's own account and deleting the account together with
 * the whole budget, against real PostgreSQL with the Flyway schema and its foreign keys.
 *
 * <p>Assertions read the HTTP response or a fresh state from the repositories.</p>
 */
class AccountApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ACCOUNT = "/api/account";
  private static final String WITH_HOUSEHOLD = ACCOUNT + "/with-household";
  private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private HouseholdRepository householdRepository;

  private Household household;
  private Member filip;
  private ExpenseCategory food;

  @BeforeEach
  void setUpHousehold() {
    household = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    food = fixtures.expenseCategory(household, "Jedzenie", true);
  }

  @Nested
  @DisplayName("Deleting only my account")
  class DeleteAccount {

    @Test
    @DisplayName("keeps my transactions in the budget, signed as a deleted account")
    void keepsTransactionsAnonymised() throws Exception {
      var klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
      var transaction = fixtures.transaction(klaudia, TransactionType.EXPENSE, food.getId(), "40", DATE, true);

      deleteAccount(klaudia, ACCOUNT, TestFixtures.PASSWORD).andExpect(status().isNoContent());

      assertThat(transactionRepository.findById(transaction.getId())).isPresent();
      mockMvc.perform(get("/api/household").with(auth.as(filip)))
             .andExpect(jsonPath("$.formerMembers[0].id").value(klaudia.getId()))
             .andExpect(jsonPath("$.formerMembers[0].deleted").value(true))
             .andExpect(jsonPath("$.formerMembers[0].email").doesNotExist());
    }

    @Test
    @DisplayName("frees the e-mail, so the same person can register again")
    void freesEmail() throws Exception {
      var klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
      deleteAccount(klaudia, ACCOUNT, TestFixtures.PASSWORD).andExpect(status().isNoContent());

      mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(new RegisterRequest("Klaudia", "klaudia@planner.test", "nowehaslo1"))))
             .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a deleted account cannot be restored by the rest of the household (HTTP 409)")
    void cannotBeRestored() throws Exception {
      var klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
      deleteAccount(klaudia, ACCOUNT, TestFixtures.PASSWORD).andExpect(status().isNoContent());

      mockMvc.perform(patch("/api/household/member/restore").param("memberId", klaudia.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("refuses a wrong password with HTTP 400 and keeps the account")
    void refusesWrongPassword() throws Exception {
      fixtures.member(household, "Klaudia", "klaudia@planner.test");

      deleteAccount(filip, ACCOUNT, "zle-haslo")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.msg").value(containsString("hasło")));

      assertThat(memberRepository.findById(filip.getId()).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("refuses the last person in the budget with HTTP 409 — they must delete the budget too")
    void refusesLastMember() throws Exception {
      deleteAccount(filip, ACCOUNT, TestFixtures.PASSWORD).andExpect(status().isConflict());

      assertThat(memberRepository.findById(filip.getId()).orElseThrow().isActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("Deleting my account and the budget")
  class DeleteAccountWithHousehold {

    @Test
    @DisplayName("removes the household with all its data, including accounts removed earlier")
    void removesEverything() throws Exception {
      fixtures.removedMember(household, "Babcia", "babcia@planner.test");
      fixtures.transaction(filip, TransactionType.EXPENSE, food.getId(), "40", DATE, false);
      fixtures.savingsGoal(household, "Wakacje", "0");

      deleteAccount(filip, WITH_HOUSEHOLD, TestFixtures.PASSWORD).andExpect(status().isNoContent());

      assertThat(householdRepository.existsById(household.getId())).isFalse();
      assertThat(memberRepository.count()).isZero();
      assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("leaves other households untouched")
    void keepsOtherHouseholds() throws Exception {
      var otherHousehold = fixtures.household();
      fixtures.member(otherHousehold, "Obcy", "obcy@planner.test");

      deleteAccount(filip, WITH_HOUSEHOLD, TestFixtures.PASSWORD).andExpect(status().isNoContent());

      assertThat(householdRepository.existsById(otherHousehold.getId())).isTrue();
      assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses while other people still use the budget (HTTP 409)")
    void refusesWhenOthersRemain() throws Exception {
      fixtures.member(household, "Klaudia", "klaudia@planner.test");

      deleteAccount(filip, WITH_HOUSEHOLD, TestFixtures.PASSWORD).andExpect(status().isConflict());

      assertThat(householdRepository.existsById(household.getId())).isTrue();
    }
  }

  private ResultActions deleteAccount(Member member, String path, String password) throws Exception {
    return mockMvc.perform(delete(path).with(auth.as(member))
                                       .contentType(APPLICATION_JSON)
                                       .content("{\"password\": \"%s\"}".formatted(password)));
  }

}
