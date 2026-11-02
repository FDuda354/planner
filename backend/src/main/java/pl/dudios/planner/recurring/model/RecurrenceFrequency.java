package pl.dudios.planner.recurring.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Period;

@Getter
@RequiredArgsConstructor
public enum RecurrenceFrequency {
  MONTHLY(Period.ofMonths(1)),
  YEARLY(Period.ofYears(1));

  private final Period interval;
}
