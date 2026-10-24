package pl.dudios.planner.summary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.summary.model.MonthlyAmount;
import pl.dudios.planner.summary.model.dto.GoalProgressDTO;
import pl.dudios.planner.summary.model.dto.ItemAmountDTO;
import pl.dudios.planner.summary.model.dto.MemberAmountDTO;
import pl.dudios.planner.summary.model.dto.MonthSummaryDTO;
import pl.dudios.planner.summary.model.dto.MonthlyContributionDTO;
import pl.dudios.planner.summary.repository.SummaryRepository;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SummaryService {

  private final SummaryRepository summaryRepository;
  private final BudgetConfigService budgetConfigService;

  @Transactional(readOnly = true)
  public List<MonthSummaryDTO> getMonthSummaries(Long householdId) {
    Map<YearMonth, List<MonthlyAmount>> byMonth = groupByMonth(summaryRepository.findMonthlyAmounts(householdId));
    return byMonth.keySet().stream()
      .sorted(Comparator.reverseOrder())
      .map(month -> toMonthSummary(month, byMonth.get(month)))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<MonthSummaryDTO> getYearSummary(Long householdId, int year) {
    List<MonthlyAmount> amounts = summaryRepository.findMonthlyAmountsBetween(householdId, Year.of(year).atDay(1), Year.of(year + 1).atDay(1));
    Map<YearMonth, List<MonthlyAmount>> byMonth = groupByMonth(amounts);
    return IntStream.rangeClosed(1, 12)
      .mapToObj(month -> YearMonth.of(year, month))
      .map(month -> toMonthSummary(month, byMonth.getOrDefault(month, List.of())))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<GoalProgressDTO> getGoalProgress(Long householdId) {
    List<MonthlyAmount> savings = summaryRepository.findMonthlyAmounts(householdId).stream()
      .filter(amount -> amount.type() == TransactionType.SAVING)
      .toList();
    return budgetConfigService.getSavingsGoals(householdId).stream()
      .map(goal -> toGoalProgress(goal, savings))
      .toList();
  }

  private MonthSummaryDTO toMonthSummary(YearMonth month, List<MonthlyAmount> amounts) {
    return new MonthSummaryDTO(
      month.toString(),
      total(amounts, amount -> amount.type() == TransactionType.INCOME),
      total(amounts, amount -> amount.type() == TransactionType.EXPENSE),
      total(amounts, amount -> amount.type() == TransactionType.SAVING),
      amountsByItem(amounts, TransactionType.EXPENSE),
      amountsByItem(amounts, TransactionType.INCOME),
      amountsByItem(amounts, TransactionType.SAVING),
      personalExpensesByMember(amounts),
      total(amounts, MonthlyAmount::isSharedExpense)
    );
  }

  private GoalProgressDTO toGoalProgress(SavingsGoal goal, List<MonthlyAmount> savings) {
    List<MonthlyAmount> deposits = savings.stream()
      .filter(amount -> amount.itemId().equals(goal.getId()))
      .toList();
    List<MonthlyContributionDTO> contributions = groupByMonth(deposits).entrySet().stream()
      .map(entry -> new MonthlyContributionDTO(entry.getKey().toString(), sum(entry.getValue())))
      .toList();
    return new GoalProgressDTO(goal.toDTO(), goal.getInitialAmount().add(sum(deposits)), contributions);
  }

  private List<ItemAmountDTO> amountsByItem(List<MonthlyAmount> amounts, TransactionType type) {
    return sumBy(amounts, amount -> amount.type() == type, MonthlyAmount::itemId).entrySet().stream()
      .map(entry -> new ItemAmountDTO(entry.getKey(), entry.getValue()))
      .sorted(Comparator.comparing(ItemAmountDTO::amount).reversed())
      .toList();
  }

  private List<MemberAmountDTO> personalExpensesByMember(List<MonthlyAmount> amounts) {
    return sumBy(amounts, MonthlyAmount::isPersonalExpense, MonthlyAmount::memberId).entrySet().stream()
      .map(entry -> new MemberAmountDTO(entry.getKey(), entry.getValue()))
      .sorted(Comparator.comparing(MemberAmountDTO::amount).reversed())
      .toList();
  }

  private Map<Long, BigDecimal> sumBy(List<MonthlyAmount> amounts, Predicate<MonthlyAmount> filter, Function<MonthlyAmount, Long> key) {
    return amounts.stream()
      .filter(filter)
      .collect(Collectors.groupingBy(key, Collectors.reducing(BigDecimal.ZERO, MonthlyAmount::amount, BigDecimal::add)));
  }

  private BigDecimal total(List<MonthlyAmount> amounts, Predicate<MonthlyAmount> filter) {
    return sum(amounts.stream().filter(filter).toList());
  }

  private BigDecimal sum(List<MonthlyAmount> amounts) {
    return amounts.stream()
      .map(MonthlyAmount::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Map<YearMonth, List<MonthlyAmount>> groupByMonth(List<MonthlyAmount> amounts) {
    return amounts.stream().collect(Collectors.groupingBy(MonthlyAmount::yearMonth, TreeMap::new, Collectors.toList()));
  }

}
