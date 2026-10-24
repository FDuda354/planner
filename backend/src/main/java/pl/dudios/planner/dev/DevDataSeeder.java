package pl.dudios.planner.dev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import pl.dudios.planner.transaction.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static pl.dudios.planner.utils.AppTimeZone.POLAND;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

  private static final String DEV_PASSWORD = "budzet123";

  private final HouseholdRepository householdRepository;
  private final MemberRepository memberRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;
  private final IncomeSourceRepository incomeSourceRepository;
  private final SavingsGoalRepository savingsGoalRepository;
  private final TransactionRepository transactionRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (memberRepository.count() > 0) {
      return;
    }
    Long householdId = householdRepository.save(Household.builder().name("Nasz dom").build()).getId();
    Member filip = saveMember(householdId, "Filip", "filip@planner.test", "#0d9488");
    Member klaudia = saveMember(householdId, "Klaudia", "klaudia@planner.test", "#c026d3");

    DevHousehold household = new DevHousehold(
      householdId, filip.getId(), klaudia.getId(),
      saveExpenseCategories(householdId), saveIncomeSources(householdId), saveSavingsGoals(householdId, filip, klaudia)
    );
    transactionRepository.saveAll(new DevTransactionGenerator(household).generate(LocalDate.now(POLAND)));
    log.info("Seeded dev household {} with members filip@planner.test and klaudia@planner.test", householdId);
  }

  private Member saveMember(Long householdId, String firstName, String email, String color) {
    return memberRepository.save(Member.builder()
                                       .householdId(householdId)
                                       .firstName(firstName)
                                       .email(email)
                                       .password(passwordEncoder.encode(DEV_PASSWORD))
                                       .color(color)
                                       .active(true)
                                       .build());
  }

  private Map<String, ExpenseCategory> saveExpenseCategories(Long householdId) {
    Map<String, ExpenseCategory> categories = new LinkedHashMap<>();
    categories.put(DevTransactionGenerator.HOUSING, expenseCategory(householdId, "Mieszkanie", "pi-home", "#6366f1"));
    categories.put(DevTransactionGenerator.BILLS, expenseCategory(householdId, "Rachunki", "pi-bolt", "#f59e0b"));
    categories.put(DevTransactionGenerator.FOOD, expenseCategory(householdId, "Jedzenie", "pi-shopping-cart", "#10b981"));
    categories.put(DevTransactionGenerator.TRANSPORT, expenseCategory(householdId, "Transport", "pi-car", "#3b82f6"));
    categories.put(DevTransactionGenerator.RESTAURANTS, expenseCategory(householdId, "Na mieście", "pi-face-smile", "#f97316"));
    categories.put(DevTransactionGenerator.HEALTH, expenseCategory(householdId, "Zdrowie", "pi-heart", "#ec4899"));
    categories.put(DevTransactionGenerator.FUN, expenseCategory(householdId, "Rozrywka", "pi-ticket", "#8b5cf6"));
    categories.put(DevTransactionGenerator.SUBSCRIPTIONS, expenseCategory(householdId, "Subskrypcje", "pi-video", "#64748b"));
    categories.put(DevTransactionGenerator.CLOTHES, expenseCategory(householdId, "Ubrania", "pi-shopping-bag", "#d946ef"));
    categories.put(DevTransactionGenerator.HOME, expenseCategory(householdId, "Dom i chemia", "pi-wrench", "#78716c"));
    categories.put(DevTransactionGenerator.GIFTS, expenseCategory(householdId, "Prezenty", "pi-gift", "#e11d48"));
    categories.put(DevTransactionGenerator.TRAVEL, expenseCategory(householdId, "Podróże", "pi-globe", "#06b6d4"));
    expenseCategoryRepository.saveAll(categories.values());
    return categories;
  }

  private Map<String, IncomeSource> saveIncomeSources(Long householdId) {
    Map<String, IncomeSource> sources = new LinkedHashMap<>();
    sources.put(DevTransactionGenerator.SALARY_FILIP, incomeSource(householdId, "Pensja Filipa", "pi-briefcase", "#059669"));
    sources.put(DevTransactionGenerator.SALARY_KLAUDIA, incomeSource(householdId, "Pensja Klaudii", "pi-briefcase", "#0891b2"));
    sources.put(DevTransactionGenerator.BONUS, incomeSource(householdId, "Premie", "pi-trophy", "#ca8a04"));
    sources.put(DevTransactionGenerator.OTHER_INCOME, incomeSource(householdId, "Inne wpływy", "pi-wallet", "#65a30d"));
    incomeSourceRepository.saveAll(sources.values());
    return sources;
  }

  private Map<String, SavingsGoal> saveSavingsGoals(Long householdId, Member filip, Member klaudia) {
    Map<String, SavingsGoal> goals = new LinkedHashMap<>();
    goals.put(DevTransactionGenerator.SAFETY_NET, savingsGoal(householdId, "Poduszka finansowa", "pi-shield", "#0ea5e9", "6 miesięcy spokojnego życia", "12000", "40000", null, true));
    goals.put(DevTransactionGenerator.JAPAN, savingsGoal(householdId, "Wakacje w Japonii", "pi-sun", "#f97316", "Wiosna 2027, kwitnące wiśnie", "2000", "18000", null, true));
    goals.put(DevTransactionGenerator.CAR, savingsGoal(householdId, "Nowy samochód", "pi-car", "#8b5cf6", "Wkład własny", "5000", "45000", filip.getId(), true));
    goals.put(DevTransactionGenerator.CAMERA, savingsGoal(householdId, "Nowy aparat", "pi-camera", "#ec4899", "Bezlusterkowiec z obiektywem", "800", "7500", klaudia.getId(), true));
    goals.put(DevTransactionGenerator.BATHROOM, savingsGoal(householdId, "Remont łazienki", "pi-hammer", "#78716c", "Wstrzymane", "0", "20000", null, false));
    savingsGoalRepository.saveAll(goals.values());
    return goals;
  }

  private ExpenseCategory expenseCategory(Long householdId, String name, String icon, String color) {
    return ExpenseCategory.builder().householdId(householdId).name(name).icon(icon).color(color).active(true).build();
  }

  private IncomeSource incomeSource(Long householdId, String name, String icon, String color) {
    return IncomeSource.builder().householdId(householdId).name(name).icon(icon).color(color).active(true).build();
  }

  private SavingsGoal savingsGoal(Long householdId, String name, String icon, String color, String note,
                                  String initialAmount, String targetAmount, Long ownerId, boolean active) {
    return SavingsGoal.builder()
                      .householdId(householdId)
                      .name(name)
                      .icon(icon)
                      .color(color)
                      .active(active)
                      .note(note)
                      .initialAmount(new BigDecimal(initialAmount))
                      .targetAmount(new BigDecimal(targetAmount))
                      .ownerId(ownerId)
                      .build();
  }

}
