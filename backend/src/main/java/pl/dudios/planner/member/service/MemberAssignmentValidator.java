package pl.dudios.planner.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberAssignmentValidator {

  private final MemberRepository memberRepository;

  public void validateAssignment(Long householdId, Long memberId, Long currentMemberId) {
    Member member = memberRepository.findByIdAndHouseholdId(memberId, householdId)
                                    .orElseThrow(() -> new RequestValidationException("Wybrana osoba nie należy do Waszego budżetu"));
    if (!member.isActive() && !Objects.equals(currentMemberId, memberId)) {
      throw new RequestValidationException("Nie można wybrać osoby usuniętej z budżetu");
    }
  }

}
