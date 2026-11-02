package pl.dudios.planner.budgetconfig.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.budgetconfig.model.BudgetItem;
import pl.dudios.planner.transaction.model.TransactionType;
import pl.dudios.planner.transaction.model.dto.ItemUsageDTO;
import pl.dudios.planner.transaction.service.TransactionService;

@Service
@RequiredArgsConstructor
public class BudgetItemRemovalService {

  private final BudgetConfigService budgetConfigService;
  private final TransactionService transactionService;

  @Transactional(readOnly = true)
  public ItemUsageDTO getItemUsage(Long householdId, TransactionType type, Long itemId) {
    BudgetItem item = budgetConfigService.getItem(householdId, type, itemId);
    return transactionService.getItemUsage(householdId, type, item.getId());
  }

  @Transactional
  public void deleteItemWithTransactions(Long householdId, TransactionType type, Long itemId) {
    transactionService.deleteItemTransactions(householdId, type, itemId);
    budgetConfigService.deleteItem(householdId, type, itemId);
  }

}
