package pl.dudios.planner.security.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.dudios.planner.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final MemberRepository memberRepository;

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    return memberRepository.findByEmail(email)
                           .orElseThrow(() -> new UsernameNotFoundException("Member with email " + email + " not found"));
  }

}
