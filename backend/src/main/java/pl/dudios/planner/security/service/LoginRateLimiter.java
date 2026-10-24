package pl.dudios.planner.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginRateLimiter {

  private static final int MAX_ATTEMPTS = 15;
  private static final Duration WINDOW = Duration.ofSeconds(60);

  private final Map<String, Deque<Instant>> failedAttempts = new ConcurrentHashMap<>();

  public boolean isAllowed(String key) {
    String normalized = normalize(key);
    Deque<Instant> timestamps = failedAttempts.get(normalized);
    if (timestamps == null) {
      return true;
    }
    synchronized (timestamps) {
      pruneExpired(timestamps);
      return timestamps.size() < MAX_ATTEMPTS;
    }
  }

  public void recordFailure(String key) {
    String normalized = normalize(key);
    Deque<Instant> timestamps = failedAttempts.computeIfAbsent(normalized, _ -> new ArrayDeque<>());
    synchronized (timestamps) {
      pruneExpired(timestamps);
      timestamps.addLast(Instant.now());
      log.warn("Login failure #{} for key '{}'", timestamps.size(), normalized);
    }
  }

  public void reset(String key) {
    failedAttempts.remove(normalize(key));
  }

  private void pruneExpired(Deque<Instant> timestamps) {
    Instant windowStart = Instant.now().minus(WINDOW);
    while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
      timestamps.pollFirst();
    }
  }

  private String normalize(String key) {
    return key == null ? "" : key.toLowerCase().trim();
  }

}
