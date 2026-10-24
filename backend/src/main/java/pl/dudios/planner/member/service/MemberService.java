package pl.dudios.planner.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dudios.planner.exception.DuplicateResourceException;
import pl.dudios.planner.household.service.HouseholdService;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.RegisterRequest;
import pl.dudios.planner.member.repository.MemberRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

  private static final List<String> MEMBER_COLORS = List.of("#0d9488", "#c026d3", "#ea580c", "#2563eb", "#65a30d", "#db2777");

  private final MemberRepository memberRepository;
  private final HouseholdService householdService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Member register(RegisterRequest request) {
    if (memberRepository.existsByEmail(request.email())) {
      throw new DuplicateResourceException("Konto z tym adresem e-mail już istnieje");
    }
    Long householdId = householdService.resolveHouseholdForNewMember(request.email());

    return memberRepository.save(Member.builder()
                                       .householdId(householdId)
                                       .firstName(request.firstName())
                                       .email(request.email())
                                       .password(passwordEncoder.encode(request.password()))
                                       .color(nextColor(householdId))
                                       .active(true)
                                       .build());
  }

  public boolean isActive(Long memberId) {
    return memberRepository.existsByIdAndActiveTrue(memberId);
  }

  private String nextColor(Long householdId) {
    Set<String> usedColors = memberRepository.findAllByHouseholdIdOrderById(householdId).stream()
                                             .map(Member::getColor)
                                             .collect(Collectors.toSet());
    return MEMBER_COLORS.stream()
                        .filter(color -> !usedColors.contains(color))
                        .findFirst()
                        .orElse(MEMBER_COLORS.get(usedColors.size() % MEMBER_COLORS.size()));
  }

}
