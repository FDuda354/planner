package pl.dudios.planner.recurring.model;

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
import pl.dudios.planner.recurring.model.dto.RecurringTransactionDTO;
import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "RECURRING_TRANSACTIONS")
public class RecurringTransaction {

  @Id
  @SequenceGenerator(
    name = "recurring_transaction_id_sequence",
    sequenceName = "recurring_transaction_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "recurring_transaction_id_sequence"
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

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "frequency", nullable = false)
  private RecurrenceFrequency frequency = RecurrenceFrequency.MONTHLY;

  @Column(name = "day_of_month", nullable = false)
  private int dayOfMonth;

  @Column(name = "month_of_year")
  private Integer monthOfYear;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "shared", nullable = false)
  private boolean shared;

  @Column(name = "note", nullable = false)
  private String note;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "next_occurrence", nullable = false)
  private LocalDate nextOccurrence;

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

  public void scheduleFrom(LocalDate today) {
    LocalDate reference = nextOccurrence == null || nextOccurrence.isBefore(today) ? today : nextOccurrence;
    LocalDate candidate = occurrenceInPeriodOf(reference);
    nextOccurrence = candidate.isBefore(today) ? occurrenceAfter(candidate) : candidate;
  }

  public boolean belongsToNobody() {
    return type == TransactionType.INCOME && shared;
  }

  public boolean isDueOn(LocalDate today) {
    return active && !nextOccurrence.isAfter(today);
  }

  public Transaction toTransaction() {
    Transaction transaction = Transaction.builder()
                                         .householdId(householdId)
                                         .amount(amount)
                                         .date(nextOccurrence)
                                         .memberId(memberId)
                                         .shared(shared)
                                         .note(note)
                                         .recurringTransactionId(id)
                                         .build();
    transaction.assignItem(type, getItemId());
    return transaction;
  }

  public void advanceToNextOccurrence() {
    nextOccurrence = occurrenceAfter(nextOccurrence);
  }

  public RecurringTransactionDTO toDTO() {
    return new RecurringTransactionDTO(id, type, getItemId(), amount, frequency, dayOfMonth, monthOfYear, memberId, shared, note, active, nextOccurrence);
  }

  private LocalDate occurrenceAfter(LocalDate occurrence) {
    return occurrenceInPeriodOf(occurrence.plus(frequency.getInterval()));
  }

  private LocalDate occurrenceInPeriodOf(LocalDate date) {
    YearMonth month = frequency == RecurrenceFrequency.MONTHLY ? YearMonth.from(date) : YearMonth.of(date.getYear(), monthOfYear);
    return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
  }

}
