package pl.dudios.planner.budgetconfig.model.dto;

import java.math.BigDecimal;

public record SavingsGoalDTO(
  Long id,
  String name,
  String icon,
  String color,
  boolean active,
  String note,
  BigDecimal initialAmount,
  BigDecimal targetAmount,
  Long ownerId
) {

}
