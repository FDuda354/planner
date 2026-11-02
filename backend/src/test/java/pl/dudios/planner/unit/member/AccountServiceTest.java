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
import pl.dudios.planner.exception.ForbiddenOperationException;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.household.service.HouseholdRemovalService;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.member.service.AccountService;
import pl.dudios.planner.member.service.MemberAssignmentValidator;
import pl.dudios.planner.transaction.repository.TransactionRepository;
import pl.dudios.planner.transaction.service.TransactionDetailsValidator;
import pl.dudios.planner.transaction.service.TransactionService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link AccountService}: deleting only one's own account (erasing personal data, keeping the
 * budget history) versus deleting the account together with the whole budget.
 *
 * <p>Only repositories are mocked. {@link HouseholdRemovalService}, {@link TransactionService},
 * {@link BudgetConfigService} and the BCrypt encoder (low cost factor) are real instances.</p>
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final Long HOUSEHOLD_ID = 10L;
  private static final Long FILIP_ID = 1L;
  private static final String PASSWORD = "budzet123";

  @Mock
  private MemberRepository memberRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private InvitationRepository invitationRepository;
  @Mock
  private HouseholdRepository householdRepository;
  @Mock
  private ExpenseCategoryRepository expenseCategoryRepository;
  @Mock
  private IncomeSourceRepository incomeSourceRepository;
  @Mock
  private SavingsGoalRepository savingsGoalRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
  private AccountService accountService;
  private Member filip;

  @BeforeEach
  void setUp() {
    var memberAssignmentValidator = new MemberAssignmentValidator(memberRepository);
    var budgetConfigService = new BudgetConfigService(expenseCategoryRepository, incomeSourceRepository, savingsGoalRepository, memberAssignmentValidator);
    var transactionService = new TransactionService(transactionRepository, new TransactionDetailsValidator(budgetConfigService, memberAssignmentValidator));
    var householdRemovalService = new HouseholdRemovalService(transactionService, budgetConfigService, invitationRepository, memberRepository, householdRepository);
    accountService = new AccountService(memberRepository, passwordEncoder, householdRemovalService);
    filip = Member.builder()
                  .id(FILIP_ID)
                  .householdId(HOUSEHOLD_ID)
                  .firstName("Filip")
                  .email("filip@planner.test")
                  .password(passwordEncoder.encode(PASSWORD))
                  .color("#0d9488")
                  .active(true)
                  .build();
    given(memberRepository.findById(FILIP_ID)).willReturn(Optional.of(filip));
  }

  @Nested
  @DisplayName("Deleting only my account")
  class DeleteAccount {

    @Test
    @DisplayName("erases name, e-mail and password and blocks access while the member row stays for the budget history")
    void erasesPersonalData() {
      // Given
      givenOtherActiveMembers(true);

      // When
      accountService.deleteAccount(FILIP_ID, PASSWORD);

      // Then
      assertThat(filip.isDeleted()).isTrue();
      assertThat(filip.isActive()).isFalse();
      assertThat(filip.getEmail()).isNull();
      assertThat(filip.getPassword()).isNull();
      assertThat(filip.getFirstName()).isNotEqualTo("Filip");
      then(householdRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("refuses when the member is the last active person — the budget would be left without anyone")
    void refusesLastActiveMember() {
      // Given
      givenOtherActiveMembers(false);

      // When / Then
      assertThatThrownBy(() -> accountService.deleteAccount(FILIP_ID, PASSWORD))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("razem z budżetem");
      assertThat(filip.isDeleted()).isFalse();
      assertThat(filip.getEmail()).isEqualTo("filip@planner.test");
    }

    @Test
    @DisplayName("refuses a wrong password and changes nothing")
    void refusesWrongPassword() {
      // Given
      var wrongPassword = "zle-haslo";

      // When / Then
      assertThatThrownBy(() -> accountService.deleteAccount(FILIP_ID, wrongPassword))
        .isInstanceOf(RequestValidationException.class)
        .hasMessageContaining("hasło");
      assertThat(filip.isDeleted()).isFalse();
      assertThat(filip.isActive()).isTrue();
    }
  }

  @Nested
  @DisplayName("Deleting my account together with the budget")
  class DeleteAccountWithHousehold {

    @Test
    @DisplayName("removes the whole household of the last active member")
    void removesHousehold() {
      // Given
      givenOtherActiveMembers(false);

      // When
      accountService.deleteAccountWithHousehold(FILIP_ID, PASSWORD);

      // Then
      then(transactionRepository).should().deleteAllByHouseholdId(HOUSEHOLD_ID);
      then(memberRepository).should().deleteAllByHouseholdId(HOUSEHOLD_ID);
      then(householdRepository).should().deleteById(HOUSEHOLD_ID);
    }

    @Test
    @DisplayName("refuses while other people still use the budget, so nobody loses data without consent")
    void refusesWhenOthersRemain() {
      // Given
      givenOtherActiveMembers(true);

      // When / Then
      assertThatThrownBy(() -> accountService.deleteAccountWithHousehold(FILIP_ID, PASSWORD))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("inne osoby");
      then(householdRepository).shouldHaveNoInteractions();
      then(transactionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("refuses a wrong password and deletes nothing")
    void refusesWrongPassword() {
      // Given
      var wrongPassword = "zle-haslo";

      // When / Then
      assertThatThrownBy(() -> accountService.deleteAccountWithHousehold(FILIP_ID, wrongPassword))
        .isInstanceOf(RequestValidationException.class);
      then(householdRepository).shouldHaveNoInteractions();
      then(memberRepository).should(never()).deleteAllByHouseholdId(anyLong());
    }
  }

  private void givenOtherActiveMembers(boolean exist) {
    given(memberRepository.existsByHouseholdIdAndActiveTrueAndIdNot(HOUSEHOLD_ID, FILIP_ID)).willReturn(exist);
  }

}
