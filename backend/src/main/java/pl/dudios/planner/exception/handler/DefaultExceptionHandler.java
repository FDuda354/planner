package pl.dudios.planner.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.dudios.planner.exception.DuplicateResourceException;
import pl.dudios.planner.exception.ForbiddenOperationException;
import pl.dudios.planner.exception.RequestValidationException;
import pl.dudios.planner.exception.ResourceNotFoundException;
import pl.dudios.planner.exception.TooManyAttemptsException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static pl.dudios.planner.utils.AppTimeZone.POLAND;

@Slf4j
@ControllerAdvice
public class DefaultExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleException(ResourceNotFoundException e, HttpServletRequest request) {
    log.warn("ResourceNotFoundException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, e.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(InsufficientAuthenticationException.class)
  public ResponseEntity<ApiError> handleException(InsufficientAuthenticationException e, HttpServletRequest request) {
    return buildApiError(request, "Zaloguj się, aby kontynuować", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleException(BadCredentialsException e, HttpServletRequest request) {
    log.debug("BadCredentialsException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, "Nieprawidłowy e-mail lub hasło", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ApiError> handleException(DisabledException e, HttpServletRequest request) {
    log.info("DisabledException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, "To konto zostało usunięte z budżetu", HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiError> handleException(DuplicateResourceException e, HttpServletRequest request) {
    log.warn("DuplicateResourceException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, e.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleException(DataIntegrityViolationException e, HttpServletRequest request) {
    log.warn("DataIntegrityViolationException on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
    return buildApiError(request, "Ktoś właśnie zmienił te same dane — odśwież stronę i spróbuj ponownie", HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ForbiddenOperationException.class)
  public ResponseEntity<ApiError> handleException(ForbiddenOperationException e, HttpServletRequest request) {
    log.warn("ForbiddenOperationException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, e.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(RequestValidationException.class)
  public ResponseEntity<ApiError> handleException(RequestValidationException e, HttpServletRequest request) {
    log.warn("RequestValidationException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, e.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiError> handleException(AuthorizationDeniedException e, HttpServletRequest request) {
    log.warn("AuthorizationDeniedException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, "Brak dostępu", HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(TooManyAttemptsException.class)
  public ResponseEntity<ApiError> handleException(TooManyAttemptsException e, HttpServletRequest request) {
    log.warn("TooManyAttemptsException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleException(MethodArgumentNotValidException e, HttpServletRequest request) {
    String message = e.getBindingResult().getFieldErrors().stream()
                      .map(FieldError::getDefaultMessage)
                      .collect(Collectors.joining("; "));
    log.warn("MethodArgumentNotValidException on {}: {}", request.getRequestURI(), message);
    return buildApiError(request, message, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiError> handleException(HandlerMethodValidationException e, HttpServletRequest request) {
    log.warn("HandlerMethodValidationException on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, "Niepoprawne parametry żądania", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ApiError> handleUnreadableRequest(Exception e, HttpServletRequest request) {
    log.warn("Unreadable request on {}: {}", request.getRequestURI(), e.getMessage());
    return buildApiError(request, "Niepoprawny format danych", HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public Object handleException(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri != null
        && !uri.startsWith("/api/")
        && !uri.startsWith("/actuator/")
        && !uri.contains(".")) {
      return "forward:/index.html";
    }
    return buildApiError(request, "Nie znaleziono zasobu", HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ClientAbortException.class)
  public void handleException(ClientAbortException e, HttpServletRequest request) {
    log.debug("Client aborted request to {}: {}", request.getRequestURI(), e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
    log.error("Unhandled exception on {}", request.getRequestURI(), e);
    return buildApiError(request, "Wewnętrzny błąd serwera", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static ResponseEntity<ApiError> buildApiError(HttpServletRequest request, String message, HttpStatus status) {
    ApiError body = new ApiError(request.getRequestURI(), message, status.value(), LocalDateTime.now(POLAND));
    return new ResponseEntity<>(body, status);
  }

}
