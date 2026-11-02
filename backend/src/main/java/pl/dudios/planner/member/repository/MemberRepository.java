package pl.dudios.planner.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.dudios.planner.member.model.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

  Optional<Member> findByEmail(String email);

  Optional<Member> findByIdAndHouseholdId(Long id, Long householdId);

  List<Member> findAllByHouseholdIdOrderById(Long householdId);

  boolean existsByEmail(String email);

  boolean existsByIdAndActiveTrue(Long id);

  boolean existsByHouseholdIdAndActiveTrueAndIdNot(Long householdId, Long id);

  @Modifying
  @Query("delete from Member m where m.householdId = :householdId")
  void deleteAllByHouseholdId(@Param("householdId") Long householdId);

}
