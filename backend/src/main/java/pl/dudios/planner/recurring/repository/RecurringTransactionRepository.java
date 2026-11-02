package pl.dudios.planner.recurring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.recurring.model.RecurringTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

  List<RecurringTransaction> findAllByHouseholdIdOrderByDayOfMonthAscIdAsc(Long householdId);

  Optional<RecurringTransaction> findByIdAndHouseholdId(Long id, Long householdId);

  List<RecurringTransaction> findAllByActiveTrueAndNextOccurrenceLessThanEqual(LocalDate date);

}
