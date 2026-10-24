package pl.dudios.planner.household.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.household.model.dto.HouseholdDTO;
import pl.dudios.planner.household.model.request.InvitationRequest;
import pl.dudios.planner.household.service.HouseholdService;
import pl.dudios.planner.member.model.Member;

@RestController
@RequestMapping("api/household")
@RequiredArgsConstructor
public class HouseholdController {

  private final HouseholdService householdService;

  @GetMapping
  public ResponseEntity<HouseholdDTO> getHousehold(@AuthenticationPrincipal Member member) {
    return ResponseEntity.ok(householdService.getHousehold(member.getHouseholdId()));
  }

  @PostMapping("/invitation")
  public ResponseEntity<Void> inviteMember(@Valid @RequestBody InvitationRequest request,
                                           @AuthenticationPrincipal Member member) {
    householdService.inviteMember(member, request.email());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/invitation")
  public ResponseEntity<Void> cancelInvitation(@RequestParam String email,
                                               @AuthenticationPrincipal Member member) {
    householdService.cancelInvitation(member.getHouseholdId(), email);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/member")
  public ResponseEntity<Void> removeMember(@RequestParam Long memberId,
                                           @AuthenticationPrincipal Member member) {
    householdService.removeMember(member, memberId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/member/restore")
  public ResponseEntity<Void> restoreMember(@RequestParam Long memberId,
                                            @AuthenticationPrincipal Member member) {
    householdService.restoreMember(member.getHouseholdId(), memberId);
    return ResponseEntity.noContent().build();
  }

}
