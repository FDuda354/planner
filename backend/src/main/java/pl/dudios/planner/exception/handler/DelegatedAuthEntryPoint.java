package pl.dudios.planner.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class DelegatedAuthEntryPoint implements AuthenticationEntryPoint {

  private final HandlerExceptionResolver handler;

  public DelegatedAuthEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handler) {
    this.handler = handler;
  }

  @Override
  public void commence(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AuthenticationException authException) {
    handler.resolveException(request, response, null, authException);
  }

}
