package pl.dudios.planner.member.model;

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
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "MEMBERS")
public class Member implements UserDetails {

  @Id
  @SequenceGenerator(
    name = "member_id_sequence",
    sequenceName = "member_id_sequence",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "member_id_sequence"
  )
  private Long id;

  @Column(name = "household_id", nullable = false)
  private Long householdId;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "color", nullable = false)
  private String color;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
  }

  @Override
  public @NonNull String getPassword() {
    return password;
  }

  @Override
  public @NonNull String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  public MemberDTO toDTO() {
    return new MemberDTO(id, firstName, email, color);
  }

}
