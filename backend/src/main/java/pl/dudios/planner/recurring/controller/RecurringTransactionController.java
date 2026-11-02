package pl.dudios.planner.recurring.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.recurring.model.dto.RecurringTransactionDTO;
import pl.dudios.planner.recurring.model.request.RecurringTransactionRequest;
import pl.dudios.planner.recurring.service.RecurringTransactionService;

import java.util.List;

@RestController
@RequestMapping("api/recurring-transaction")
@RequiredArgsConstructor
public class RecurringTransactionController {

  private final RecurringTransactionService recurringTransactionService;

  @GetMapping
  public ResponseEntity<List<RecurringTransactionDTO>> getRecurringTransactions(@AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(recurringTransactionService.getRecurringTransactions(member.getHouseholdId()));
  }

  @PostMapping
  public ResponseEntity<Void> saveRecurringTransaction(@Valid @RequestBody RecurringTransactionRequest request,
                                                       @AuthenticationPrincipal Member member) {
    recurringTransactionService.saveRecurringTransaction(member.getHouseholdId(), request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteRecurringTransaction(@RequestParam Long recurringTransactionId,
                                                         @AuthenticationPrincipal Member member) {
    recurringTransactionService.deleteRecurringTransaction(member.getHouseholdId(), recurringTransactionId);
    return ResponseEntity.noContent().build();
  }

}
