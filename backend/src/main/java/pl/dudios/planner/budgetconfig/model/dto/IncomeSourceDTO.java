package pl.dudios.planner.budgetconfig.model.dto;

public record IncomeSourceDTO(
  Long id,
  String name,
  String icon,
  String color,
  boolean active
) {

}
