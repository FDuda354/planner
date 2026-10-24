package pl.dudios.planner.household.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationRequest(
  @NotBlank(message = "Podaj adres e-mail")
  @Email(message = "Niepoprawny adres e-mail")
  @Size(max = 150, message = "Adres e-mail jest za długi")
  String email
) {

  public InvitationRequest {
    email = email == null ? null : email.trim().toLowerCase();
  }

}
