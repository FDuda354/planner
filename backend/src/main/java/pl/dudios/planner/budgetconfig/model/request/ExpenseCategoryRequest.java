package pl.dudios.planner.budgetconfig.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExpenseCategoryRequest(
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

  boolean active
) {

  public ExpenseCategoryRequest {
    name = name == null ? null : name.trim();
  }

}
