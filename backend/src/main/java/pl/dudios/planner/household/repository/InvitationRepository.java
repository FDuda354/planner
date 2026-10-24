package pl.dudios.planner.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.household.model.Invitation;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  Optional<Invitation> findByEmail(String email);

  List<Invitation> findAllByHouseholdIdOrderById(Long householdId);

  long deleteByHouseholdIdAndEmail(Long householdId, String email);

}
