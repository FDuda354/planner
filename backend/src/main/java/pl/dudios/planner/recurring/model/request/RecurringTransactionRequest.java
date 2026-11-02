package pl.dudios.planner.recurring.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.dudios.planner.recurring.model.RecurrenceFrequency;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;

public record RecurringTransactionRequest(
  Long id,

  @NotNull(message = "Wybierz rodzaj transakcji")
  TransactionType type,

  @NotNull(message = "Wybierz kategorię")
  Long itemId,

  @NotNull(message = "Podaj kwotę")
  @DecimalMin(value = "0.01", message = "Kwota musi być większa od zera")
  @Digits(integer = 10, fraction = 2, message = "Niepoprawna kwota")
  BigDecimal amount,

  @NotNull(message = "Wybierz, jak często naliczać")
  RecurrenceFrequency frequency,

  @NotNull(message = "Wybierz dzień miesiąca")
  @Min(value = 1, message = "Dzień miesiąca musi być z zakresu 1–31")
  @Max(value = 31, message = "Dzień miesiąca musi być z zakresu 1–31")
  Integer dayOfMonth,

  @Min(value = 1, message = "Miesiąc musi być z zakresu 1–12")
  @Max(value = 12, message = "Miesiąc musi być z zakresu 1–12")
  Integer monthOfYear,

  @NotNull(message = "Wybierz, czyja to transakcja")
  Long memberId,

  boolean shared,

  @Size(max = 255, message = "Notatka może mieć maksymalnie 255 znaków")
  String note,

  boolean active
) {

  public RecurringTransactionRequest {
    note = note == null ? "" : note.trim();
  }

}
