package pl.dudios.planner.integration.household;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.integration.AbstractIntegrationTest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@code /api/household}: invitations and removing / restoring members of a budget.
 *
 * <p>Assertions read the HTTP response or a fresh state from the repositories.</p>
 */
class HouseholdApiIntegrationTest extends AbstractIntegrationTest {

  private static final String ROOT = "/api/household";

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private InvitationRepository invitationRepository;

  private Household household;
  private Member filip;
  private Member klaudia;

  @BeforeEach
  void setUpHousehold() {
    household = fixtures.household();
    filip = fixtures.member(household, "Filip", "filip@planner.test");
    klaudia = fixtures.member(household, "Klaudia", "klaudia@planner.test");
  }

  @Nested
  @DisplayName("Invitations")
  class Invitations {

    @Test
    @DisplayName("an invitation shows up as pending, stored with a normalized e-mail")
    void invitesNewPerson() throws Exception {
      invite(" Mama@Planner.TEST ").andExpect(status().isNoContent());

      mockMvc.perform(get(ROOT).with(auth.as(klaudia)))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.pendingInvitations", contains("mama@planner.test")));
    }

    @Test
    @DisplayName("cancelling removes the pending invitation")
    void cancelsInvitation() throws Exception {
      invite("mama@planner.test").andExpect(status().isNoContent());

      mockMvc.perform(delete(ROOT + "/invitation").param("email", "mama@planner.test").with(auth.as(filip)))
             .andExpect(status().isNoContent());

      assertThat(invitationRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses to invite someone who already has an account with HTTP 409")
    void refusesExistingAccount() throws Exception {
      fixtures.member(fixtures.household(), "Obcy", "obcy@planner.test");

      invite("obcy@planner.test").andExpect(status().isConflict());

      assertThat(invitationRepository.count()).isZero();
    }

    @Test
    @DisplayName("refuses an invalid e-mail with HTTP 400")
    void refusesInvalidEmail() throws Exception {
      invite("to-nie-email").andExpect(status().isBadRequest());

      assertThat(invitationRepository.count()).isZero();
    }
  }

  @Nested
  @DisplayName("Removing and restoring members")
  class RemovingMembers {

    @Test
    @DisplayName("a removed member moves to the former members and can be restored")
    void removesAndRestores() throws Exception {
      mockMvc.perform(delete(ROOT + "/member").param("memberId", klaudia.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNoContent());

      mockMvc.perform(get(ROOT).with(auth.as(filip)))
             .andExpect(jsonPath("$.members", hasSize(1)))
             .andExpect(jsonPath("$.formerMembers[0].email").value("klaudia@planner.test"));

      mockMvc.perform(patch(ROOT + "/member/restore").param("memberId", klaudia.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNoContent());

      assertThat(memberRepository.findById(klaudia.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("a member cannot remove themselves (HTTP 409)")
    void cannotRemoveSelf() throws Exception {
      mockMvc.perform(delete(ROOT + "/member").param("memberId", filip.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isConflict());

      assertThat(memberRepository.findById(filip.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("a member of another household cannot be removed (HTTP 404)")
    void cannotRemoveStranger() throws Exception {
      var stranger = fixtures.member(fixtures.household(), "Obcy", "obcy@planner.test");

      mockMvc.perform(delete(ROOT + "/member").param("memberId", stranger.getId().toString()).with(auth.as(filip)))
             .andExpect(status().isNotFound());

      assertThat(memberRepository.findById(stranger.getId()).orElseThrow().isActive()).isTrue();
    }
  }

  private ResultActions invite(String email) throws Exception {
    return mockMvc.perform(post(ROOT + "/invitation").with(auth.as(filip))
                                                     .contentType(APPLICATION_JSON)
                                                     .content("{\"email\": \"%s\"}".formatted(email)));
  }

}
