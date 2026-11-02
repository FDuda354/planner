package pl.dudios.planner.household.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.household.repository.HouseholdRepository;
import pl.dudios.planner.household.repository.InvitationRepository;
import pl.dudios.planner.member.repository.MemberRepository;
import pl.dudios.planner.transaction.service.TransactionService;

@Service
@RequiredArgsConstructor
public class HouseholdRemovalService {

  private final TransactionService transactionService;
  private final BudgetConfigService budgetConfigService;
  private final InvitationRepository invitationRepository;
  private final MemberRepository memberRepository;
  private final HouseholdRepository householdRepository;

  @Transactional
  public void deleteHousehold(Long householdId) {
    transactionService.deleteHouseholdTransactions(householdId);
    invitationRepository.deleteAllByHouseholdId(householdId);
    budgetConfigService.deleteHouseholdConfig(householdId);
    memberRepository.deleteAllByHouseholdId(householdId);
    householdRepository.deleteById(householdId);
  }

}
