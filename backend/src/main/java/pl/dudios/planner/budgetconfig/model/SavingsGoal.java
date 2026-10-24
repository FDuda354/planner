package pl.dudios.planner.budgetconfig.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import pl.dudios.planner.budgetconfig.model.dto.SavingsGoalDTO;

import java.math.BigDecimal;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

@Entity
@Table(name = "SAVINGS_GOALS")
public class SavingsGoal extends BudgetItem {

  @Id
  @SequenceGenerator(
    name = "savings_goal_id_sequence",
    sequenceName = "savings_goal_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "savings_goal_id_sequence"
  )
  private Long id;

  @Column(name = "note", nullable = false)
  private String note;

  @Column(name = "initial_amount", nullable = false)
  private BigDecimal initialAmount;

  @Column(name = "target_amount", nullable = false)
  private BigDecimal targetAmount;

  @Column(name = "owner_id")
  private Long ownerId;

  public SavingsGoalDTO toDTO() {
    return new SavingsGoalDTO(id, getName(), getIcon(), getColor(), isActive(), note, initialAmount, targetAmount, ownerId);
  }

}
