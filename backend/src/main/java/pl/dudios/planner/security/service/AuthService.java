package pl.dudios.planner.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pl.dudios.planner.exception.TooManyAttemptsException;
import pl.dudios.planner.security.model.AuthRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final LoginRateLimiter loginRateLimiter;

  public Authentication login(AuthRequest authRequest) {
    if (!loginRateLimiter.isAllowed(authRequest.email())) {
      log.error("Rate limit hit for email '{}'", authRequest.email());
      throw new TooManyAttemptsException("Zbyt wiele nieudanych prób logowania. Spróbuj ponownie za minutę.");
    }

    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
        authRequest.email(),
        authRequest.password())
      );
    } catch (BadCredentialsException e) {
      log.warn("Bad credentials for email '{}'", authRequest.email());
      loginRateLimiter.recordFailure(authRequest.email());
      throw e;
    }

    loginRateLimiter.reset(authRequest.email());
    return authentication;
  }

}
