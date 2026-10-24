package pl.dudios.planner.unit.member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.DuplicateResourceException;
import pl.dudios.planner.household.model.Invitation;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.household.service.HouseholdService;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.RegisterRequest;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.member.service.MemberService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link MemberService#register}: creating an account and placing it in a household.
 *
 * <p>Repositories are mocked. {@link HouseholdService}, {@link BudgetConfigService} and the BCrypt
 * password encoder are real instances (the encoder with a low cost factor to keep the test fast).</p>
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;

  @Mock
  private MemberRepository memberRepository;
  @Mock
  private HouseholdRepository householdRepository;
  @Mock
  private InvitationRepository invitationRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
  private MemberService memberService;

  @BeforeEach
  void setUp() {
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberRepository);
    var householdService = new HouseholdService(householdRepository, invitationRepository, memberRepository, budgetConfigService);
    memberService = new MemberService(memberRepository, householdService, passwordEncoder);
  }

  @Nested
  @DisplayName("Registering an account")
  class Register {

    @Test
    @DisplayName("creates an active member in the inviting household with an encoded password")
    void registersInvitedMember() {
      // Given
      givenInvitationFor("klaudia@planner.test");
      given(memberRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of());
      given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

      // When
      var member = memberService.register(new RegisterRequest(" Klaudia ", " Klaudia@Planner.TEST ", "budzet123"));

      // Then
      assertThat(member.getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
      assertThat(member.getFirstName()).isEqualTo("Klaudia");
      assertThat(member.getEmail()).isEqualTo("klaudia@planner.test");
      assertThat(member.isActive()).isTrue();
      assertThat(member.getPassword()).isNotEqualTo("budzet123");
      assertThat(passwordEncoder.matches("budzet123", member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("gives the new member a colour nobody in the household uses yet")
    void picksUnusedColour() {
      // Given
      givenInvitationFor("klaudia@planner.test");
      given(memberRepository.findAllByHouseholdIdOrderById(HOUSEHOLD_ID)).willReturn(List.of(memberWithColour("#0d9488")));
      given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

      // When
      var member = memberService.register(new RegisterRequest("Klaudia", "klaudia@planner.test", "budzet123"));

      // Then
      assertThat(member.getColor()).isNotEqualTo("#0d9488");
    }

    @Test
    @DisplayName("refuses an e-mail that already has an account and creates nothing")
    void refusesDuplicateEmail() {
      // Given
      given(memberRepository.existsByEmail("filip@planner.test")).willReturn(true);

      // When / Then
      assertThatThrownBy(() -> memberService.register(new RegisterRequest("Filip", "filip@planner.test", "budzet123")))
        .isInstanceOf(DuplicateResourceException.class);
      then(memberRepository).should(never()).save(any());
      then(householdRepository).shouldHaveNoInteractions();
      then(invitationRepository).shouldHaveNoInteractions();
    }
  }

  private void givenInvitationFor(String email) {
    given(invitationRepository.findByEmail(email))
      .willReturn(Optional.of(Invitation.builder().householdId(HOUSEHOLD_ID).email(email).invitedBy(1L).build()));
  }

  private Member memberWithColour(String colour) {
    return Member.builder().id(1L).householdId(HOUSEHOLD_ID).firstName("Filip").email("filip@planner.test").password("hash").color(colour).active(true).build();
  }

}
