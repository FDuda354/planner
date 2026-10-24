package pl.dudios.planner.security.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
  @NotBlank(message = "Podaj adres e-mail")
  @Size(max = 150, message = "Adres e-mail jest za długi")
  String email,

  @NotBlank(message = "Podaj hasło")
  @Size(max = 200, message = "Hasło jest za długie")
  String password
) {

  public AuthRequest {
    email = email == null ? null : email.trim().toLowerCase();
  }

}
