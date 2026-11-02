package pl.dudios.planner.transaction.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.member.service.MemberAssignmentValidator;
import pl.dudios.planner.transaction.model.request.TransactionDetails;

@Service
@RequiredArgsConstructor
public class TransactionDetailsValidator {

  private final BudgetConfigService budgetConfigService;
  private final MemberAssignmentValidator memberAssignmentValidator;

  public void validate(Long householdId, TransactionDetails details, Long currentMemberId) {
    validateItemBelongsToHousehold(householdId, details);
    validateSharedFlag(details);
    memberAssignmentValidator.validateAssignment(householdId, details.memberId(), currentMemberId);
  }

  private void validateItemBelongsToHousehold(Long householdId, TransactionDetails details) {
    budgetConfigService.getItem(householdId, details.type(), details.itemId());
  }

  private void validateSharedFlag(TransactionDetails details) {
    if (details.shared() && !details.type().canBeShared()) {
      throw new RequestValidationException("Oszczędności nie można oznaczyć jako wspólnej");
    }
  }

}
