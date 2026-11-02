package pl.dudios.planner.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dudios.planner.member.model.AccountDeletionRequest;
import pl.dudios.planner.member.model.Member;
import pl.dudios.planner.member.service.AccountService;

@RestController
@RequestMapping("api/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;
  private final LogoutHandler logoutHandler = new SecurityContextLogoutHandler();

  @DeleteMapping
  public ResponseEntity<Void> deleteAccount(@Valid @RequestBody AccountDeletionRequest request,
                                            @AuthenticationPrincipal Member member,
                                            Authentication authentication,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
    accountService.deleteAccount(member.getId(), request.password());
    logoutHandler.logout(httpRequest, httpResponse, authentication);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/with-household")
  public ResponseEntity<Void> deleteAccountWithHousehold(@Valid @RequestBody AccountDeletionRequest request,
                                                         @AuthenticationPrincipal Member member,
                                                         Authentication authentication,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
    accountService.deleteAccountWithHousehold(member.getId(), request.password());
    logoutHandler.logout(httpRequest, httpResponse, authentication);
    return ResponseEntity.noContent().build();
  }

}
