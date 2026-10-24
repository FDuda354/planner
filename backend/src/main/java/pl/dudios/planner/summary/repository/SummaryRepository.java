package pl.dudios.planner.summary.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.summary.model.MonthlyAmount;
import pl.dudios.planner.transaction.model.Transaction;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SummaryRepository extends org.springframework.data.repository.Repository<Transaction, Long> {

  String SELECT_MONTHLY_AMOUNTS = """
    select new pl.dudios.planner.summary.model.MonthlyAmount(
      year(t.date), month(t.date), t.type,
      coalesce(t.expenseCategoryId, t.incomeSourceId, t.savingsGoalId),
      t.memberId, t.shared, sum(t.amount))
    from Transaction t
    where t.householdId = :householdId
    """;

  String GROUP_BY_MONTH_AND_ITEM = """
    group by year(t.date), month(t.date), t.type,
      coalesce(t.expenseCategoryId, t.incomeSourceId, t.savingsGoalId),
      t.memberId, t.shared
    """;

  @Query(SELECT_MONTHLY_AMOUNTS + GROUP_BY_MONTH_AND_ITEM)
  List<MonthlyAmount> findMonthlyAmounts(@Param("householdId") Long householdId);

  @Query(SELECT_MONTHLY_AMOUNTS + "and t.date >= :from and t.date < :to\n" + GROUP_BY_MONTH_AND_ITEM)
  List<MonthlyAmount> findMonthlyAmountsBetween(@Param("householdId") Long householdId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

}
