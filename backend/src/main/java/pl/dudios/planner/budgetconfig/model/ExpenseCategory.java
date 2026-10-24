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
import pl.dudios.planner.budgetconfig.model.dto.ExpenseCategoryDTO;


import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

@Entity
@Table(name = "EXPENSE_CATEGORIES")
public class ExpenseCategory extends BudgetItem {

  @Id
  @SequenceGenerator(
    name = "expense_category_id_sequence",
    sequenceName = "expense_category_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "expense_category_id_sequence"
  )
  private Long id;

  public ExpenseCategoryDTO toDTO() {
    return new ExpenseCategoryDTO(id, getName(), getIcon(), getColor(), isActive());
  }

}
