package pl.dudios.planner.member.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
  @NotBlank(message = "Podaj imię")
  @Size(max = 60, message = "Imię może mieć maksymalnie 60 znaków")
  String firstName,

  @NotBlank(message = "Podaj adres e-mail")
  @Email(message = "Niepoprawny adres e-mail")
  @Size(max = 150, message = "Adres e-mail jest za długi")
  String email,

  @NotBlank(message = "Podaj hasło")
  @Size(min = 8, max = 200, message = "Hasło musi mieć od 8 do 200 znaków")
  String password
) {

  public RegisterRequest {
    firstName = firstName == null ? null : firstName.trim();
    email = email == null ? null : email.trim().toLowerCase();
  }

}
