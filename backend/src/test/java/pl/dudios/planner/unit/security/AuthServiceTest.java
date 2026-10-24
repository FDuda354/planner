package pl.dudios.planner.unit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pl.dudios.planner.exception.TooManyAttemptsException;
import pl.dudios.planner.security.model.AuthRequest;
import pl.dudios.planner.security.service.AuthService;
import pl.dudios.planner.security.service.LoginRateLimiter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Unit tests for {@link AuthService#login}: authentication guarded by the brute-force rate limiter.
 *
 * <p>The {@link AuthenticationManager} is mocked as the boundary to Spring Security; the
 * {@link LoginRateLimiter} is a real instance, so its counting rules are exercised for real.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String EMAIL = "filip@planner.test";
  private static final int ALLOWED_FAILURES = 15;

  @Mock
  private AuthenticationManager authenticationManager;

  private LoginRateLimiter loginRateLimiter;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    loginRateLimiter = new LoginRateLimiter();
    authService = new AuthService(authenticationManager, loginRateLimiter);
  }

  @Test
  @DisplayName("returns the authentication produced for valid credentials")
  void logsIn() {
    // Given
    var authentication = UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of());
    given(authenticationManager.authenticate(any())).willReturn(authentication);

    // When
    var result = authService.login(new AuthRequest(EMAIL, "budzet123"));

    // Then
    assertThat(result).isSameAs(authentication);
  }

  @Test
  @DisplayName("propagates bad credentials and counts the failed attempt")
  void countsFailedAttempt() {
    // Given
    given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("bad"));

    // When
    for (int attempt = 0; attempt < ALLOWED_FAILURES; attempt++) {
      assertThatThrownBy(() -> authService.login(new AuthRequest(EMAIL, "wrong")))
        .isInstanceOf(BadCredentialsException.class);
    }

    // Then
    assertThat(loginRateLimiter.isAllowed(EMAIL)).isFalse();
  }

  @Test
  @DisplayName("blocks further attempts after too many failures without asking Spring Security")
  void blocksAfterTooManyFailures() {
    // Given
    for (int attempt = 0; attempt < ALLOWED_FAILURES; attempt++) {
      loginRateLimiter.recordFailure(EMAIL);
    }

    // When / Then
    assertThatThrownBy(() -> authService.login(new AuthRequest(EMAIL, "budzet123")))
      .isInstanceOf(TooManyAttemptsException.class);
    then(authenticationManager).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("clears earlier failures after a successful login")
  void resetsCounterAfterSuccess() {
    // Given
    for (int attempt = 0; attempt < ALLOWED_FAILURES - 1; attempt++) {
      loginRateLimiter.recordFailure(EMAIL);
    }
    given(authenticationManager.authenticate(any())).willReturn(UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of()));

    // When
    authService.login(new AuthRequest(EMAIL, "budzet123"));
    loginRateLimiter.recordFailure(EMAIL);

    // Then
    assertThat(loginRateLimiter.isAllowed(EMAIL)).isTrue();
  }

  @Test
  @DisplayName("does not count a failure that is not about credentials, e.g. a removed account")
  void doesNotCountOtherAuthenticationErrors() {
    // Given
    given(authenticationManager.authenticate(any())).willThrow(new DisabledException("removed"));

    // When
    for (int attempt = 0; attempt < ALLOWED_FAILURES; attempt++) {
      assertThatThrownBy(() -> authService.login(new AuthRequest(EMAIL, "budzet123")))
        .isInstanceOf(DisabledException.class);
    }

    // Then
    assertThat(loginRateLimiter.isAllowed(EMAIL)).isTrue();
  }

}
