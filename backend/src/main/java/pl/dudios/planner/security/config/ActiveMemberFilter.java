package pl.dudios.planner.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.service.MemberService;

import java.io.IOException;

@RequiredArgsConstructor
public class ActiveMemberFilter extends OncePerRequestFilter {

  private final MemberService memberService;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    if (isRemovedMember(SecurityContextHolder.getContext().getAuthentication())) {
      endSession(request);
    }
    filterChain.doFilter(request, response);
  }

  private boolean isRemovedMember(Authentication authentication) {
    return authentication != null
           && authentication.getPrincipal() instanceof Member member
           && !memberService.isActive(member.getId());
  }

  private void endSession(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

}
