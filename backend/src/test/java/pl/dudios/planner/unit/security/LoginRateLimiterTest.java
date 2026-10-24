package pl.dudios.planner.unit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.dudios.planner.security.service.LoginRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoginRateLimiter}: brute-force protection of the login form.
 *
 * <p>No collaborators — the limiter is a plain in-memory component tested as a real instance.</p>
 */
class LoginRateLimiterTest {

  private static final String EMAIL = "filip@planner.test";
  private static final int ALLOWED_FAILURES = 15;

  private LoginRateLimiter loginRateLimiter;

  @BeforeEach
  void setUp() {
    loginRateLimiter = new LoginRateLimiter();
  }

  @Test
  @DisplayName("allows logging in for an account without failed attempts")
  void allowsFreshAccount() {
    // Given
    var email = EMAIL;

    // When
    var allowed = loginRateLimiter.isAllowed(email);

    // Then
    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("still allows the last attempt before the limit is reached")
  void allowsBelowLimit() {
    // Given
    recordFailures(EMAIL, ALLOWED_FAILURES - 1);

    // When
    var allowed = loginRateLimiter.isAllowed(EMAIL);

    // Then
    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("blocks the account once the limit of failed attempts is reached")
  void blocksAtLimit() {
    // Given
    recordFailures(EMAIL, ALLOWED_FAILURES);

    // When
    var allowed = loginRateLimiter.isAllowed(EMAIL);

    // Then
    assertThat(allowed).isFalse();
  }

  @Test
  @DisplayName("treats e-mails differing only in letter case and spaces as the same account")
  void normalizesKey() {
    // Given
    recordFailures("  Filip@Planner.TEST ", ALLOWED_FAILURES);

    // When
    var allowed = loginRateLimiter.isAllowed(EMAIL);

    // Then
    assertThat(allowed).isFalse();
  }

  @Test
  @DisplayName("does not block other accounts")
  void isolatesAccounts() {
    // Given
    recordFailures(EMAIL, ALLOWED_FAILURES);

    // When
    var allowed = loginRateLimiter.isAllowed("klaudia@planner.test");

    // Then
    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("unblocks the account after a reset")
  void resetUnblocks() {
    // Given
    recordFailures(EMAIL, ALLOWED_FAILURES);

    // When
    loginRateLimiter.reset(EMAIL);

    // Then
    assertThat(loginRateLimiter.isAllowed(EMAIL)).isTrue();
  }

  private void recordFailures(String email, int count) {
    for (int attempt = 0; attempt < count; attempt++) {
      loginRateLimiter.recordFailure(email);
    }
  }

}
