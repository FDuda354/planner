package pl.dudios.planner.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.exception.ForbiddenOperationException;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.household.service.HouseholdRemovalService;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final HouseholdRemovalService householdRemovalService;

  @Transactional
  public void deleteAccount(Long memberId, String password) {
    Member member = getConfirmedMember(memberId, password);
    rejectLastActiveMember(member);
    member.eraseAccount();
  }

  @Transactional
  public void deleteAccountWithHousehold(Long memberId, String password) {
    Member member = getConfirmedMember(memberId, password);
    rejectOtherActiveMembers(member);
    householdRemovalService.deleteHousehold(member.getHouseholdId());
  }

  private Member getConfirmedMember(Long memberId, String password) {
    Member member = memberRepository.findById(memberId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono konta"));
    if (!passwordEncoder.matches(password, member.getPassword())) {
      throw new RequestValidationException("Nieprawidłowe hasło");
    }
    return member;
  }

  private void rejectLastActiveMember(Member member) {
    if (!hasOtherActiveMembers(member)) {
      throw new ForbiddenOperationException("Jesteś jedyną osobą w budżecie — usuń konto razem z budżetem");
    }
  }

  private void rejectOtherActiveMembers(Member member) {
    if (hasOtherActiveMembers(member)) {
      throw new ForbiddenOperationException("W budżecie są jeszcze inne osoby — usuń je z budżetu albo usuń tylko swoje konto");
    }
  }

  private boolean hasOtherActiveMembers(Member member) {
    return memberRepository.existsByHouseholdIdAndActiveTrueAndIdNot(member.getHouseholdId(), member.getId());
  }

}
