package pl.dudios.planner.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.transaction.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  List<Transaction> findAllByHouseholdIdAndDateBetweenOrderByDateDescIdDesc(Long householdId, LocalDate from, LocalDate to);

  Optional<Transaction> findByIdAndHouseholdId(Long id, Long householdId);

}
