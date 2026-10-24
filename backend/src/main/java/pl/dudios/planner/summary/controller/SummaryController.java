package pl.dudios.planner.summary.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.summary.model.dto.GoalProgressDTO;
import pl.dudios.planner.summary.model.dto.MonthSummaryDTO;
import pl.dudios.planner.summary.service.SummaryService;

import java.util.List;

@RestController
@RequestMapping("api/summary")
@RequiredArgsConstructor
public class SummaryController {

  private final SummaryService summaryService;

  @GetMapping("/months")
  public ResponseEntity<List<MonthSummaryDTO>> getMonthSummaries(@AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(summaryService.getMonthSummaries(member.getHouseholdId()));
  }

  @GetMapping("/year")
  public ResponseEntity<List<MonthSummaryDTO>> getYearSummary(@RequestParam @Min(2000) @Max(2100) int year,
                                                              @AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(summaryService.getYearSummary(member.getHouseholdId(), year));
  }

  @GetMapping("/goals")
  public ResponseEntity<List<GoalProgressDTO>> getGoalProgress(@AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(summaryService.getGoalProgress(member.getHouseholdId()));
  }

}
