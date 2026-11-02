package pl.dudios.planner.member.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountDeletionRequest(
  @NotBlank(message = "Podaj hasło, aby potwierdzić usunięcie")
  @Size(max = 200, message = "Hasło jest za długie")
  String password
) {

}
