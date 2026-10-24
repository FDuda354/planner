package pl.dudios.planner.household.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.DuplicateResourceException;
import pl.dudios.planner.exception.ForbiddenOperationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.household.model.Household;
import pl.dudios.planner.household.model.Invitation;
import pl.dudios.planner.household.model.dto.HouseholdDTO;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.repository.MemberRepository;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HouseholdService {

  private static final String DEFAULT_HOUSEHOLD_NAME = "Nasz dom";

  private final HouseholdRepository householdRepository;
  private final InvitationRepository invitationRepository;
  private final MemberRepository memberRepository;
  private final BudgetConfigService budgetConfigService;

  @Transactional(readOnly = true)
  public HouseholdDTO getHousehold(Long householdId) {
    Household household = householdRepository.findById(householdId)
                                             .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono budżetu domowego"));
    List<Member> members = memberRepository.findAllByHouseholdIdOrderById(householdId);

    return new HouseholdDTO(
      household.getId(),
      household.getName(),
      members.stream().filter(Member::isActive).map(Member::toDTO).toList(),
      members.stream().filter(member -> !member.isActive()).map(Member::toDTO).toList(),
      invitationRepository.findAllByHouseholdIdOrderById(householdId).stream().map(Invitation::getEmail).toList()
    );
  }

  @Transactional
  public Long resolveHouseholdForNewMember(String email) {
    return invitationRepository.findByEmail(email)
                               .map(this::acceptInvitation)
                               .orElseGet(this::createHousehold);
  }

  @Transactional
  public void inviteMember(Member inviter, String email) {
    rejectExistingAccount(inviter.getHouseholdId(), email);
    rejectPendingInvitation(inviter.getHouseholdId(), email);

    invitationRepository.save(Invitation.builder()
                                        .householdId(inviter.getHouseholdId())
                                        .email(email)
                                        .invitedBy(inviter.getId())
                                        .build());
  }

  @Transactional
  public void cancelInvitation(Long householdId, String email) {
    if (invitationRepository.deleteByHouseholdIdAndEmail(householdId, email.trim().toLowerCase()) == 0) {
      throw new ResourceNotFoundException("Nie znaleziono zaproszenia dla " + email);
    }
  }

  @Transactional
  public void removeMember(Member remover, Long memberId) {
    if (Objects.equals(remover.getId(), memberId)) {
      throw new ForbiddenOperationException("Nie możesz usunąć samego siebie z budżetu");
    }
    getMember(remover.getHouseholdId(), memberId).setActive(false);
  }

  @Transactional
  public void restoreMember(Long householdId, Long memberId) {
    getMember(householdId, memberId).setActive(true);
  }

  private Member getMember(Long householdId, Long memberId) {
    return memberRepository.findByIdAndHouseholdId(memberId, householdId)
                           .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono domownika"));
  }

  private Long acceptInvitation(Invitation invitation) {
    invitationRepository.delete(invitation);
    return invitation.getHouseholdId();
  }

  private Long createHousehold() {
    Household household = householdRepository.save(Household.builder().name(DEFAULT_HOUSEHOLD_NAME).build());
    budgetConfigService.createDefaultConfig(household.getId());
    return household.getId();
  }

  private void rejectExistingAccount(Long householdId, String email) {
    memberRepository.findByEmail(email).ifPresent(member -> {
      throw new DuplicateResourceException(member.getHouseholdId().equals(householdId) && member.isActive()
        ? "Ta osoba już jest w Waszym budżecie"
        : "Ta osoba ma już konto w aplikacji — zaproszenia działają tylko dla nowych kont");
    });
  }

  private void rejectPendingInvitation(Long householdId, String email) {
    invitationRepository.findByEmail(email).ifPresent(invitation -> {
      throw new DuplicateResourceException(invitation.getHouseholdId().equals(householdId)
        ? "Ta osoba ma już zaproszenie do Waszego budżetu"
        : "Ta osoba ma już zaproszenie do innego budżetu");
    });
  }

}
