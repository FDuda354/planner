package pl.dudios.planner.budgetconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.budgetconfig.model.SavingsGoal;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

  List<SavingsGoal> findAllByHouseholdIdOrderById(Long householdId);

  Optional<SavingsGoal> findByIdAndHouseholdId(Long id, Long householdId);

}
