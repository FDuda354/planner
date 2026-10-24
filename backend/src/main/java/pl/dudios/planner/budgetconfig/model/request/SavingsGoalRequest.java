package pl.dudios.planner.budgetconfig.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SavingsGoalRequest(
  Long id,

  @NotBlank(message = "Podaj nazwę")
  @Size(max = 60, message = "Nazwa może mieć maksymalnie 60 znaków")
  String name,

  @NotBlank(message = "Wybierz ikonę")
  @Pattern(regexp = "^pi-[a-z0-9-]{1,37}$", message = "Niepoprawna ikona")
  String icon,

  @NotBlank(message = "Wybierz kolor")
  @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Niepoprawny kolor")
  String color,

  boolean active,

  @Size(max = 255, message = "Notatka może mieć maksymalnie 255 znaków")
  String note,

  @NotNull(message = "Podaj kwotę odłożoną na start")
  @DecimalMin(value = "0", message = "Kwota na start nie może być ujemna")
  @Digits(integer = 10, fraction = 2, message = "Niepoprawna kwota na start")
  BigDecimal initialAmount,

  @NotNull(message = "Podaj kwotę celu")
  @DecimalMin(value = "0.01", message = "Cel musi być większy od zera")
  @Digits(integer = 10, fraction = 2, message = "Niepoprawna kwota celu")
  BigDecimal targetAmount,

  Long ownerId
) {

  public SavingsGoalRequest {
    name = name == null ? null : name.trim();
    note = note == null ? "" : note.trim();
  }

}
