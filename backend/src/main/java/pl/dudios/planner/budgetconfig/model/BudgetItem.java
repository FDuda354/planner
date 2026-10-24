package pl.dudios.planner.budgetconfig.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

@MappedSuperclass
public abstract class BudgetItem {

  @Column(name = "household_id", nullable = false)
  private Long householdId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "icon", nullable = false)
  private String icon;

  @Column(name = "color", nullable = false)
  private String color;

  @Column(name = "active", nullable = false)
  private boolean active;

  public abstract Long getId();

  public void applyDetails(String name, String icon, String color, boolean active) {
    this.name = name;
    this.icon = icon;
    this.color = color;
    this.active = active;
  }

}
