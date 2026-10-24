package pl.dudios.planner.budgetconfig.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.budgetconfig.model.IncomeSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Long> {

  List<IncomeSource> findAllByHouseholdIdOrderById(Long householdId);

  Optional<IncomeSource> findByIdAndHouseholdId(Long id, Long householdId);

}
