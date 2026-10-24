package pl.dudios.planner.transaction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.dudios.planner.transaction.model.dto.TransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "TRANSACTIONS")
public class Transaction {

  @Id
  @SequenceGenerator(
    name = "transaction_id_sequence",
    sequenceName = "transaction_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "transaction_id_sequence"
  )
  private Long id;

  @Column(name = "household_id", nullable = false)
  private Long householdId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private TransactionType type;

  @Column(name = "expense_category_id")
  private Long expenseCategoryId;

  @Column(name = "income_source_id")
  private Long incomeSourceId;

  @Column(name = "savings_goal_id")
  private Long savingsGoalId;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "transaction_date", nullable = false)
  private LocalDate date;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "shared", nullable = false)
  private boolean shared;

  @Column(name = "note", nullable = false)
  private String note;

  public Long getItemId() {
    return switch (type) {
      case EXPENSE -> expenseCategoryId;
      case INCOME -> incomeSourceId;
      case SAVING -> savingsGoalId;
    };
  }

  public void assignItem(TransactionType type, Long itemId) {
    this.type = type;
    this.expenseCategoryId = type == TransactionType.EXPENSE ? itemId : null;
    this.incomeSourceId = type == TransactionType.INCOME ? itemId : null;
    this.savingsGoalId = type == TransactionType.SAVING ? itemId : null;
  }

  public TransactionDTO toDTO() {
    return new TransactionDTO(id, type, getItemId(), amount, date, memberId, shared, note);
  }

}
