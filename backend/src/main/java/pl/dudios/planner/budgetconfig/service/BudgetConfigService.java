package pl.dudios.planner.budgetconfig.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;
import pl.dudios.planner.budgetconfig.model.IncomeSource;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.model.dto.BudgetConfigDTO;
import pl.dudios.planner.budgetconfig.model.request.ExpenseCategoryRequest;
import pl.dudios.planner.budgetconfig.model.request.IncomeSourceRequest;
import pl.dudios.planner.budgetconfig.model.request.SavingsGoalRequest;
import pl.dudios.planner.budgetconfig.repository.ExpenseCategoryRepository;
import pl.dudios.planner.budgetconfig.repository.IncomeSourceRepository;
import pl.dudios.planner.budgetconfig.repository.SavingsGoalRepository;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BudgetConfigService {

  private final ExpenseCategoryRepository expenseCategoryRepository;
  private final IncomeSourceRepository incomeSourceRepository;
  private final SavingsGoalRepository savingsGoalRepository;
  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public BudgetConfigDTO getBudgetConfig(Long householdId) {
    return new BudgetConfigDTO(
      expenseCategoryRepository.findAllByHouseholdIdOrderById(householdId).stream().map(ExpenseCategory::toDTO).toList(),
      incomeSourceRepository.findAllByHouseholdIdOrderById(householdId).stream().map(IncomeSource::toDTO).toList(),
      getSavingsGoals(householdId).stream().map(SavingsGoal::toDTO).toList()
    );
  }

  @Transactional
  public void saveExpenseCategory(Long householdId, ExpenseCategoryRequest request) {
    ExpenseCategory category = request.id() == null
      ? ExpenseCategory.builder().householdId(householdId).build()
      : getExpenseCategory(householdId, request.id());

    category.applyDetails(request.name(), request.icon(), request.color(), request.active());

    expenseCategoryRepository.save(category);
  }

  @Transactional
  public void saveIncomeSource(Long householdId, IncomeSourceRequest request) {
    IncomeSource source = request.id() == null
      ? IncomeSource.builder().householdId(householdId).build()
      : getIncomeSource(householdId, request.id());

    source.applyDetails(request.name(), request.icon(), request.color(), request.active());

    incomeSourceRepository.save(source);
  }

  @Transactional
  public void saveSavingsGoal(Long householdId, SavingsGoalRequest request) {
    SavingsGoal goal = request.id() == null
      ? SavingsGoal.builder().householdId(householdId).build()
      : getSavingsGoal(householdId, request.id());

    validateGoalOwner(householdId, goal, request.ownerId());

    goal.applyDetails(request.name(), request.icon(), request.color(), request.active());
    goal.setNote(request.note());
    goal.setInitialAmount(request.initialAmount());
    goal.setTargetAmount(request.targetAmount());
    goal.setOwnerId(request.ownerId());

    savingsGoalRepository.save(goal);
  }

  public ExpenseCategory getExpenseCategory(Long householdId, Long categoryId) {
    return expenseCategoryRepository.findByIdAndHouseholdId(categoryId, householdId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono kategorii wydatków"));
  }

  public IncomeSource getIncomeSource(Long householdId, Long sourceId) {
    return incomeSourceRepository.findByIdAndHouseholdId(sourceId, householdId)
                                 .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono źródła dochodu"));
  }

  public SavingsGoal getSavingsGoal(Long householdId, Long goalId) {
    return savingsGoalRepository.findByIdAndHouseholdId(goalId, householdId)
                                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono celu oszczędnościowego"));
  }

  public List<SavingsGoal> getSavingsGoals(Long householdId) {
    return savingsGoalRepository.findAllByHouseholdIdOrderById(householdId);
  }

  @Transactional
  public void createDefaultConfig(Long householdId) {
    expenseCategoryRepository.saveAll(DefaultBudgetConfig.EXPENSE_CATEGORIES.stream()
      .map(category -> ExpenseCategory.builder()
                                      .householdId(householdId)
                                      .name(category.name())
                                      .icon(category.icon())
                                      .color(category.color())
                                      .active(true)
                                      .build())
      .toList());

    incomeSourceRepository.saveAll(DefaultBudgetConfig.INCOME_SOURCES.stream()
      .map(source -> IncomeSource.builder()
                                 .householdId(householdId)
                                 .name(source.name())
                                 .icon(source.icon())
                                 .color(source.color())
                                 .active(true)
                                 .build())
      .toList());

    savingsGoalRepository.saveAll(DefaultBudgetConfig.SAVINGS_GOALS.stream()
      .map(goal -> SavingsGoal.builder()
                              .householdId(householdId)
                              .name(goal.name())
                              .icon(goal.icon())
                              .color(goal.color())
                              .active(true)
                              .note(goal.note())
                              .initialAmount(BigDecimal.ZERO)
                              .targetAmount(goal.targetAmount())
                              .build())
      .toList());
  }

  private void validateGoalOwner(Long householdId, SavingsGoal goal, Long ownerId) {
    if (ownerId == null || Objects.equals(goal.getOwnerId(), ownerId)) {
      return;
    }
    Member owner = memberRepository.findByIdAndHouseholdId(ownerId, householdId)
                                   .orElseThrow(() -> new RequestValidationException("Wybrana osoba nie należy do Waszego budżetu"));
    if (!owner.isActive()) {
      throw new RequestValidationException("Nie można przypisać celu do osoby usuniętej z budżetu");
    }
  }

}
