package pl.dudios.planner.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.model.MemberDTO;
import pl.dudios.planner.member.model.RegisterRequest;
import pl.dudios.planner.member.service.MemberService;
import pl.dudios.planner.security.model.AuthRequest;
import pl.dudios.planner.security.service.AuthService;

import java.util.Objects;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final MemberService memberService;
  private final SecurityContextRepository securityContextRepository;

  @PostMapping("/login")
  public ResponseEntity<MemberDTO> login(@Valid @RequestBody AuthRequest authRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
    return ResponseEntity.ok(startSession(authService.login(authRequest), request, response));
  }

  @PostMapping("/register")
  public ResponseEntity<MemberDTO> register(@Valid @RequestBody RegisterRequest registerRequest,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
    memberService.register(registerRequest);
    Authentication authentication = authService.login(new AuthRequest(registerRequest.email(), registerRequest.password()));
    return ResponseEntity.ok(startSession(authentication, request, response));
  }

  @GetMapping("/me")
  public ResponseEntity<MemberDTO> me(@AuthenticationPrincipal Member member) {
    if (member == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(member.toDTO());
  }

  private MemberDTO startSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    if (request.getSession(false) != null) {
      request.changeSessionId();
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);

    return Objects.requireNonNull((Member) authentication.getPrincipal()).toDTO();
  }

}
