package pl.dudios.planner.household.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "INVITATIONS")
public class Invitation {

  @Id
  @SequenceGenerator(
    name = "invitation_id_sequence",
    sequenceName = "invitation_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "invitation_id_sequence"
  )
  private Long id;

  @Column(name = "household_id", nullable = false)
  private Long householdId;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "invited_by", nullable = false)
  private Long invitedBy;

}
