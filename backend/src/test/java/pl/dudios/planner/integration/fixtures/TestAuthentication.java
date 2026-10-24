package pl.dudios.planner.integration.fixtures;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.dudios.planner.member.model.Member;

/**
 * Authenticates MockMvc requests as a given member for tests that are not about the login flow itself.
 *
 * <p>The principal is the {@link Member} entity, exactly what a real login leaves in the security
 * context, and a valid CSRF token is attached so mutating requests pass the CSRF filter.</p>
 */
@Component
public class TestAuthentication {

  public RequestPostProcessor as(Member member) {
    return request -> {
      var authenticated = SecurityMockMvcRequestPostProcessors.user(member).postProcessRequest(request);
      return SecurityMockMvcRequestPostProcessors.csrf().postProcessRequest(authenticated);
    };
  }

}
