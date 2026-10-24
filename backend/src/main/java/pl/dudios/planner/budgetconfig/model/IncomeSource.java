package pl.dudios.planner.budgetconfig.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import pl.dudios.planner.budgetconfig.model.dto.IncomeSourceDTO;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

@Entity
@Table(name = "INCOME_SOURCES")
public class IncomeSource extends BudgetItem {

  @Id
  @SequenceGenerator(
    name = "income_source_id_sequence",
    sequenceName = "income_source_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "income_source_id_sequence"
  )
  private Long id;

  public IncomeSourceDTO toDTO() {
    return new IncomeSourceDTO(id, getName(), getIcon(), getColor(), isActive());
  }

}
