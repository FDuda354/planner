package pl.dudios.planner.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.dto.ItemUsageDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  String WHERE_ITEM_MATCHES = """
    where t.householdId = :householdId
      and t.type = :type
      and coalesce(t.expenseCategoryId, t.incomeSourceId, t.savingsGoalId) = :itemId
    """;

  List<Transaction> findAllByHouseholdIdAndDateBetweenOrderByDateDescIdDesc(Long householdId, LocalDate from, LocalDate to);

  Optional<Transaction> findByIdAndHouseholdId(Long id, Long householdId);

  @Query("select new pl.dudios.planner.transaction.model.dto.ItemUsageDTO(count(t), sum(t.amount)) from Transaction t " + WHERE_ITEM_MATCHES)
  ItemUsageDTO findItemUsage(@Param("householdId") Long householdId,
                             @Param("type") TransactionType type,
                             @Param("itemId") Long itemId);

  @Modifying
  @Query("delete from Transaction t " + WHERE_ITEM_MATCHES)
  void deleteAllByItem(@Param("householdId") Long householdId,
                       @Param("type") TransactionType type,
                       @Param("itemId") Long itemId);

  @Modifying
  @Query("delete from Transaction t where t.householdId = :householdId")
  void deleteAllByHouseholdId(@Param("householdId") Long householdId);

}
