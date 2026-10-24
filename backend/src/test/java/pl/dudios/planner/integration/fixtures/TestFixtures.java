package pl.dudios.planner.integration.fixtures;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds persistent test data straight through the repositories, so integration tests can set up
 * a household without going through the HTTP API they are not testing.
 *
 * <p>Every member gets the password {@link #PASSWORD}; its BCrypt hash is computed once per JVM run.</p>
 */
@Component
@RequiredArgsConstructor
public class TestFixtures {

  public static final String PASSWORD = "budzet123";

  private final HouseholdRepository householdRepository;
  private final MemberRepository memberRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;
  private final IncomeSourceRepository incomeSourceRepository;
  private final SavingsGoalRepository savingsGoalRepository;
  private final TransactionRepository transactionRepository;
  private final PasswordEncoder passwordEncoder;

  private String passwordHash;

  public Household household() {
    return householdRepository.save(Household.builder().name("Nasz dom").build());
  }

  public Member member(Household household, String firstName, String email) {
    return memberRepository.save(Member.builder()
                                       .householdId(household.getId())
                                       .firstName(firstName)
                                       .email(email)
                                       .password(passwordHash())
                                       .color("#0d9488")
                                       .active(true)
                                       .build());
  }

  public Member removedMember(Household household, String firstName, String email) {
    var member = member(household, firstName, email);
    member.setActive(false);
    return memberRepository.save(member);
  }

  public ExpenseCategory expenseCategory(Household household, String name, boolean active) {
    return expenseCategoryRepository.save(ExpenseCategory.builder()
                                                         .householdId(household.getId())
                                                         .name(name)
                                                         .icon("pi-shopping-cart")
                                                         .color("#10b981")
                                                         .active(active)
                                                         .build());
  }

  public IncomeSource incomeSource(Household household, String name) {
    return incomeSourceRepository.save(IncomeSource.builder()
                                                   .householdId(household.getId())
                                                   .name(name)
                                                   .icon("pi-briefcase")
                                                   .color("#059669")
                                                   .active(true)
                                                   .build());
  }

  public SavingsGoal savingsGoal(Household household, String name, String initialAmount) {
    return savingsGoalRepository.save(SavingsGoal.builder()
                                                 .householdId(household.getId())
                                                 .name(name)
                                                 .icon("pi-sun")
                                                 .color("#f97316")
                                                 .active(true)
                                                 .note("")
                                                 .initialAmount(new BigDecimal(initialAmount))
                                                 .targetAmount(new BigDecimal("20000"))
                                                 .build());
  }

  public Transaction transaction(Member author, TransactionType type, Long itemId, String amount, LocalDate date, boolean shared) {
    var transaction = Transaction.builder()
                                 .householdId(author.getHouseholdId())
                                 .memberId(author.getId())
                                 .amount(new BigDecimal(amount))
                                 .date(date)
                                 .shared(shared)
                                 .note("")
                                 .build();
    transaction.assignItem(type, itemId);
    return transactionRepository.save(transaction);
  }

  private String passwordHash() {
    if (passwordHash == null) {
      passwordHash = passwordEncoder.encode(PASSWORD);
    }
    return passwordHash;
  }

}
