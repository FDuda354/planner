package pl.dudios.planner.unit.household;

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
import pl.dudios.planner.exception.DuplicateResourceException;
import pl.dudios.planner.exception.ForbiddenOperationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.household.model.Invitation;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.household.service.HouseholdService;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.MemberDTO;
import pl.dudios.planner.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link HouseholdService}: who belongs to a budget, how invitations work and how
 * members are removed from and restored to a household.
 *
 * <p>Only repositories are mocked. {@link BudgetConfigService} is a real instance (with mocked
 * repositories) so creating a household observably seeds the default budget configuration.</p>
 */
@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long OTHER_HOUSEHOLD_ID = 20L;

  @Mock
  private HouseholdRepository householdRepository;
  @Mock
  private InvitationRepository invitationRepository;
  @Mock
  private MemberRepository memberRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;

  private HouseholdService householdService;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    householdService = new HouseholdService(householdRepository, invitationRepository, memberRepository, budgetConfigService);
  }

  @Nested
  @DisplayName("Reading a household")
  class GetHousehold {

    @Test
    @DisplayName("lists active members separately from removed ones, together with pending invitations")
    void splitsActiveAndFormerMembers() {
      // Given
      var filip = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      var grandma = member(3L, HOUSEHOLD_ID, "babcia@planner.test", false);
      given(householdRepository.findById(HOUSEHOLD_ID)).willReturn(Optional.of(household(HOUSEHOLD_ID)));
      given(memberRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(filip, grandma));
      given(invitationRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(invitation(HOUSEHOLD_ID, "new@planner.test")));

      // When
      var result = householdService.getHousehold(HOUSEHOLD_ID);

      // Then
      assertThat(result.members()).extracting(MemberDTO::email).containsExactly("filip@planner.test");
      assertThat(result.formerMembers()).extracting(MemberDTO::email).containsExactly("babcia@planner.test");
      assertThat(result.pendingInvitations()).containsExactly("new@planner.test");
    }

    @Test
    @DisplayName("fails when the household does not exist")
    void failsForUnknownHousehold() {
      // Given
      given(householdRepository.findById(HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> householdService.getHousehold(HOUSEHOLD_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Choosing a household for a new account")
  class ResolveHouseholdForNewMember {

    @Test
    @DisplayName("joins the inviting household and consumes the invitation")
    void acceptsPendingInvitation() {
      // Given
      var invitation = invitation(HOUSEHOLD_ID, "klaudia@planner.test");
      given(invitationRepository.findByEmail("klaudia@planner.test")).willReturn(Optional.of(invitation));

      // When
      var householdId = householdService.resolveHouseholdForNewMember("klaudia@planner.test");

      // Then
      assertThat(householdId).isEqualTo(HOUSEHOLD_ID);
      then(invitationRepository).should().delete(invitation);
      then(householdRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("creates a fresh household with the default budget configuration when nobody invited the person")
    @SuppressWarnings("unchecked")
    void createsHouseholdWithoutInvitation() {
      // Given
      given(invitationRepository.findByEmail("solo@planner.test")).willReturn(Optional.empty());
      given(householdRepository.save(any(Household.class))).willAnswer(invocation -> {
        Household household = invocation.getArgument(0);
        household.setId(HOUSEHOLD_ID);
        return household;
      });

      // When
      var householdId = householdService.resolveHouseholdForNewMember("solo@planner.test");

      // Then
      assertThat(householdId).isEqualTo(HOUSEHOLD_ID);
      ArgumentCaptor<List<ExpenseCategory>> categories = ArgumentCaptor.forClass(List.class);
      then(expenseCategoryRepository).should().saveAll(categories.capture());
      assertThat(categories.getValue())
        .isNotEmpty()
        .allSatisfy(category -> assertThat(category.getHouseholdId()).isEqualTo(HOUSEHOLD_ID));
    }
  }

  @Nested
  @DisplayName("Inviting a member")
  class InviteMember {

    @Test
    @DisplayName("stores an invitation bound to the inviter's household")
    void storesInvitation() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("new@planner.test")).willReturn(Optional.empty());
      given(invitationRepository.findByEmail("new@planner.test")).willReturn(Optional.empty());

      // When
      householdService.inviteMember(inviter, "new@planner.test");

      // Then
      var saved = ArgumentCaptor.forClass(Invitation.class);
      then(invitationRepository).should().save(saved.capture());
      assertThat(saved.getValue().getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
      assertThat(saved.getValue().getEmail()).isEqualTo("new@planner.test");
      assertThat(saved.getValue().getInvitedBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("refuses to invite someone who already belongs to the budget")
    void refusesCurrentMember() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("klaudia@planner.test")).willReturn(Optional.of(member(2L, HOUSEHOLD_ID, "klaudia@planner.test", true)));

      // When / Then
      assertThatThrownBy(() -> householdService.inviteMember(inviter, "klaudia@planner.test"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("już jest");
      then(invitationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses to invite an existing account — invitations only work for new accounts")
    void refusesExistingAccountElsewhere() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("other@planner.test")).willReturn(Optional.of(member(5L, OTHER_HOUSEHOLD_ID, "other@planner.test", true)));

      // When / Then
      assertThatThrownBy(() -> householdService.inviteMember(inviter, "other@planner.test"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("ma już konto");
      then(invitationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("treats a removed member of the same budget as an existing account, not as a current member")
    void treatsRemovedMemberAsExistingAccount() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("babcia@planner.test")).willReturn(Optional.of(member(3L, HOUSEHOLD_ID, "babcia@planner.test", false)));

      // When / Then
      assertThatThrownBy(() -> householdService.inviteMember(inviter, "babcia@planner.test"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("ma już konto");
    }

    @Test
    @DisplayName("refuses a second invitation for the same e-mail in the same budget")
    void refusesDuplicateInvitation() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("new@planner.test")).willReturn(Optional.empty());
      given(invitationRepository.findByEmail("new@planner.test")).willReturn(Optional.of(invitation(HOUSEHOLD_ID, "new@planner.test")));

      // When / Then
      assertThatThrownBy(() -> householdService.inviteMember(inviter, "new@planner.test"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("Waszego budżetu");
      then(invitationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("refuses to invite a person already invited to another budget")
    void refusesInvitationToAnotherBudget() {
      // Given
      var inviter = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByEmail("new@planner.test")).willReturn(Optional.empty());
      given(invitationRepository.findByEmail("new@planner.test")).willReturn(Optional.of(invitation(OTHER_HOUSEHOLD_ID, "new@planner.test")));

      // When / Then
      assertThatThrownBy(() -> householdService.inviteMember(inviter, "new@planner.test"))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("innego budżetu");
      then(invitationRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("Cancelling an invitation")
  class CancelInvitation {

    @Test
    @DisplayName("matches the invitation regardless of e-mail letter case and surrounding spaces")
    void normalizesEmail() {
      // Given
      given(invitationRepository.deleteByHouseholdIdAndEmail(HOUSEHOLD_ID, "new@planner.test")).willReturn(1L);

      // When
      householdService.cancelInvitation(HOUSEHOLD_ID, "  New@Planner.TEST ");

      // Then
      then(invitationRepository).should().deleteByHouseholdIdAndEmail(HOUSEHOLD_ID, "new@planner.test");
    }

    @Test
    @DisplayName("fails when the budget has no such invitation")
    void failsForUnknownInvitation() {
      // Given
      given(invitationRepository.deleteByHouseholdIdAndEmail(HOUSEHOLD_ID, "ghost@planner.test")).willReturn(0L);

      // When / Then
      assertThatThrownBy(() -> householdService.cancelInvitation(HOUSEHOLD_ID, "ghost@planner.test"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("ghost@planner.test");
    }
  }

  @Nested
  @DisplayName("Removing and restoring members")
  class RemoveAndRestoreMember {

    @Test
    @DisplayName("deactivates the removed member instead of deleting their history")
    void deactivatesMember() {
      // Given
      var remover = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      var klaudia = member(2L, HOUSEHOLD_ID, "klaudia@planner.test", true);
      given(memberRepository.findByIdAndHouseholdId(2L, HOUSEHOLD_ID)).willReturn(Optional.of(klaudia));

      // When
      householdService.removeMember(remover, 2L);

      // Then
      assertThat(klaudia.isActive()).isFalse();
      then(memberRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("does not let a member remove themselves")
    void refusesSelfRemoval() {
      // Given
      var remover = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);

      // When / Then
      assertThatThrownBy(() -> householdService.removeMember(remover, 1L))
        .isInstanceOf(ForbiddenOperationException.class);
      then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cannot remove a member of another household")
    void refusesForeignMember() {
      // Given
      var remover = member(1L, HOUSEHOLD_ID, "filip@planner.test", true);
      given(memberRepository.findByIdAndHouseholdId(9L, HOUSEHOLD_ID)).willReturn(Optional.empty());

      // When / Then
      assertThatThrownBy(() -> householdService.removeMember(remover, 9L))
        .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("restoring gives a removed member their access back")
    void restoresMember() {
      // Given
      var grandma = member(3L, HOUSEHOLD_ID, "babcia@planner.test", false);
      given(memberRepository.findByIdAndHouseholdId(3L, HOUSEHOLD_ID)).willReturn(Optional.of(grandma));

      // When
      householdService.restoreMember(HOUSEHOLD_ID, 3L);

      // Then
      assertThat(grandma.isActive()).isTrue();
    }
  }

  private Member member(Long id, Long householdId, String email, boolean active) {
    return Member.builder()
                 .id(id)
                 .householdId(householdId)
                 .firstName(email.substring(0, email.indexOf('@')))
                 .email(email)
                 .password("hash")
                 .color("#0d9488")
                 .active(active)
                 .build();
  }

  private Household household(Long id) {
    return Household.builder().id(id).name("Nasz dom").build();
  }

  private Invitation invitation(Long householdId, String email) {
    return Invitation.builder().householdId(householdId).email(email).invitedBy(1L).build();
  }

}
