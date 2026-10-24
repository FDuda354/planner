package pl.dudios.planner.budgetconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.budgetconfig.model.ExpenseCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

  List<ExpenseCategory> findAllByHouseholdIdOrderById(Long householdId);

  Optional<ExpenseCategory> findByIdAndHouseholdId(Long id, Long householdId);

}
