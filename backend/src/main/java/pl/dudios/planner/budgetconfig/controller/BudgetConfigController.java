package pl.dudios.planner.budgetconfig.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.budgetconfig.model.dto.BudgetConfigDTO;
import pl.dudios.planner.budgetconfig.model.request.ExpenseCategoryRequest;
import pl.dudios.planner.budgetconfig.model.request.IncomeSourceRequest;
import pl.dudios.planner.budgetconfig.model.request.SavingsGoalRequest;
import pl.dudios.planner.budgetconfig.service.BudgetConfigService;
import pl.dudios.planner.member.model.Member;

@RestController
@RequestMapping("api/budget-config")
@RequiredArgsConstructor
public class BudgetConfigController {

  private final BudgetConfigService budgetConfigService;

  @GetMapping
  public ResponseEntity<BudgetConfigDTO> getBudgetConfig(@AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(budgetConfigService.getBudgetConfig(member.getHouseholdId()));
  }

  @PostMapping("/expense-category")
  public ResponseEntity<Void> saveExpenseCategory(@Valid @RequestBody ExpenseCategoryRequest request,
                                                  @AuthenticationPrincipal Member member) {
    budgetConfigService.saveExpenseCategory(member.getHouseholdId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/income-source")
  public ResponseEntity<Void> saveIncomeSource(@Valid @RequestBody IncomeSourceRequest request,
                                               @AuthenticationPrincipal Member member) {
    budgetConfigService.saveIncomeSource(member.getHouseholdId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/savings-goal")
  public ResponseEntity<Void> saveSavingsGoal(@Valid @RequestBody SavingsGoalRequest request,
                                              @AuthenticationPrincipal Member member) {
    budgetConfigService.saveSavingsGoal(member.getHouseholdId(), request);
    return ResponseEntity.noContent().build();
  }

}
