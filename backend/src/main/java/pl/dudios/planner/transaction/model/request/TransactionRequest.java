package pl.dudios.planner.transaction.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
  Long id,

  @NotNull(message = "Wybierz rodzaj transakcji")
  TransactionType type,

  @NotNull(message = "Wybierz kategorię")
  Long itemId,

  @NotNull(message = "Podaj kwotę")
  @DecimalMin(value = "0.01", message = "Kwota musi być większa od zera")
  @Digits(integer = 10, fraction = 2, message = "Niepoprawna kwota")
  BigDecimal amount,

  @NotNull(message = "Wybierz datę")
  LocalDate date,

  boolean shared,

  @Size(max = 255, message = "Notatka może mieć maksymalnie 255 znaków")
  String note
) {

  public TransactionRequest {
    note = note == null ? "" : note.trim();
  }

}
