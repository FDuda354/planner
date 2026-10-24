package pl.dudios.planner.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.household.model.Household;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, Long> {

}
