package pl.dudios.planner.transaction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import pl.dudios.planner.transaction.model.dto.TransactionDTO;
import pl.dudios.planner.transaction.model.request.TransactionRequest;
import pl.dudios.planner.transaction.service.TransactionService;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("api/transaction")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @GetMapping
  public ResponseEntity<List<TransactionDTO>> getTransactions(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                                              @AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(transactionService.getTransactions(member.getHouseholdId(), month));
  }

  @PostMapping
  public ResponseEntity<Void> saveTransaction(@Valid @RequestBody TransactionRequest request,
                                              @AuthenticationPrincipal Member member) {
    transactionService.saveTransaction(member, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteTransaction(@RequestParam Long transactionId,
                                                @AuthenticationPrincipal Member member) {
    transactionService.deleteTransaction(member.getHouseholdId(), transactionId);
    return ResponseEntity.noContent().build();
  }

}
