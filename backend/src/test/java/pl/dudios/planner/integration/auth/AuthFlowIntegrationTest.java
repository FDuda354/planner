package pl.dudios.planner.integration.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.integration.fixtures.TestFixtures;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.RegisterRequest;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.security.model.AuthRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of the account lifecycle through the real security filter chain: registration,
 * joining a household by invitation, logging in and out, and cutting off a member removed from the budget.
 *
 * <p>Requests carry a real HTTP session obtained from {@code /api/auth/login} or {@code /api/auth/register},
 * exactly like the browser does.</p>
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

  private static final String REGISTER = "/api/auth/register";
  private static final String LOGIN = "/api/auth/login";
  private static final String LOGOUT = "/api/auth/logout";
  private static final String ME = "/api/auth/me";
  private static final String HOUSEHOLD = "/api/household";

  @Autowired
  private MemberRepository memberRepository;

  @Nested
  @DisplayName("Registration")
  class Registration {

    @Test
    @DisplayName("creates a new budget with the default configuration and logs the person in")
    void createsOwnBudget() throws Exception {
      var session = register("Filip", "Filip@Planner.test");

      mockMvc.perform(get("/api/budget-config").session(session))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.expenseCategories").isNotEmpty())
             .andExpect(jsonPath("$.incomeSources").isNotEmpty());
      mockMvc.perform(get(HOUSEHOLD).session(session))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.members", hasSize(1)))
             .andExpect(jsonPath("$.members[0].email").value("filip@planner.test"));
    }

    @Test
    @DisplayName("an invited person joins the inviter's budget instead of creating a new one")
    void joinsInvitingBudget() throws Exception {
      var household = fixtures.household();
      var filip = fixtures.member(household, "Filip", "filip@planner.test");
      mockMvc.perform(post(HOUSEHOLD + "/invitation").with(auth.as(filip))
                                                      .contentType(APPLICATION_JSON)
                                                      .content("{\"email\": \"klaudia@planner.test\"}"))
             .andExpect(status().isNoContent());

      var session = register("Klaudia", "klaudia@planner.test");

      mockMvc.perform(get(HOUSEHOLD).session(session))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value(household.getId()))
             .andExpect(jsonPath("$.members", hasSize(2)))
             .andExpect(jsonPath("$.pendingInvitations", hasSize(0)));
    }

    @Test
    @DisplayName("refuses an e-mail that already has an account with HTTP 409")
    void refusesDuplicateEmail() throws Exception {
      fixtures.member(fixtures.household(), "Filip", "filip@planner.test");

      mockMvc.perform(post(REGISTER).contentType(APPLICATION_JSON)
                                    .content(json(new RegisterRequest("Inny", "FILIP@planner.test", "haslo1234"))))
             .andExpect(status().isConflict());

      assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses a password shorter than 8 characters with HTTP 400")
    void refusesShortPassword() throws Exception {
      mockMvc.perform(post(REGISTER).contentType(APPLICATION_JSON)
                                    .content(json(new RegisterRequest("Filip", "filip@planner.test", "krotkie"))))
             .andExpect(status().isBadRequest());

      assertThat(memberRepository.count()).isZero();
    }
  }

  @Nested
  @DisplayName("Login and logout")
  class LoginAndLogout {

    @Test
    @DisplayName("opens a session that gives access to the budget")
    void opensSession() throws Exception {
      var household = fixtures.household();
      fixtures.member(household, "Filip", "filip@planner.test");

      var session = login(" FILIP@planner.test ");

      mockMvc.perform(get(ME).session(session))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.email").value("filip@planner.test"));
      mockMvc.perform(get(HOUSEHOLD).session(session))
             .andExpect(status().isOk());
    }

    @Test
    @DisplayName("rejects a wrong password with HTTP 401")
    void rejectsWrongPassword() throws Exception {
      fixtures.member(fixtures.household(), "Filip", "filip@planner.test");

      mockMvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
                                 .content(json(new AuthRequest("filip@planner.test", "zle-haslo"))))
             .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an anonymous visitor has no current member and no access to the budget")
    void anonymousHasNoAccess() throws Exception {
      mockMvc.perform(get(ME))
             .andExpect(status().isNoContent());
      mockMvc.perform(get(HOUSEHOLD))
             .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logging out ends the session")
    void logoutEndsSession() throws Exception {
      fixtures.member(fixtures.household(), "Filip", "filip@planner.test");
      var session = login("filip@planner.test");

      mockMvc.perform(post(LOGOUT).session(session).with(csrf()))
             .andExpect(status().isNoContent());

      assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("a changing request without the CSRF token is refused even with a valid session")
    void requiresCsrfToken() throws Exception {
      fixtures.member(fixtures.household(), "Filip", "filip@planner.test");
      var session = login("filip@planner.test");

      mockMvc.perform(delete(HOUSEHOLD + "/invitation").param("email", "x@planner.test").session(session))
             .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Member removed from the budget")
  class RemovedMember {

    @Test
    @DisplayName("cannot log in, and is told why — but only after giving the right password")
    void cannotLogIn() throws Exception {
      fixtures.removedMember(fixtures.household(), "Babcia", "babcia@planner.test");

      mockMvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
                                 .content(json(new AuthRequest("babcia@planner.test", TestFixtures.PASSWORD))))
             .andExpect(status().isUnauthorized())
             .andExpect(jsonPath("$.msg").value(containsString("usunięte")));
      mockMvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
                                 .content(json(new AuthRequest("babcia@planner.test", "zle-haslo"))))
             .andExpect(status().isUnauthorized())
             .andExpect(jsonPath("$.msg").value(containsString("Nieprawidłowy")));
    }

    @Test
    @DisplayName("loses access with their very next request after being removed")
    void losesOpenSession() throws Exception {
      Household household = fixtures.household();
      Member filip = fixtures.member(household, "Filip", "filip@planner.test");
      Member klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
      var klaudiaSession = login("klaudia@planner.test");

      mockMvc.perform(delete(HOUSEHOLD + "/member").param("memberId", klaudia.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNoContent());

      mockMvc.perform(get(HOUSEHOLD).session(klaudiaSession))
             .andExpect(status().isUnauthorized());
    }
  }

  private MockHttpSession register(String firstName, String email) throws Exception {
    var result = mockMvc.perform(post(REGISTER).contentType(APPLICATION_JSON)
                                               .content(json(new RegisterRequest(firstName, email, TestFixtures.PASSWORD))))
                        .andExpect(status().isOk())
                        .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private MockHttpSession login(String email) throws Exception {
    var result = mockMvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
                                            .content(json(new AuthRequest(email, TestFixtures.PASSWORD))))
                        .andExpect(status().isOk())
                        .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private String json(Object body) {
    return objectMapper.writeValueAsString(body);
  }

}
