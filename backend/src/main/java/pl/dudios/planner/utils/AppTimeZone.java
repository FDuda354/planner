package pl.dudios.planner.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppTimeZone {

  public static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");

  public static final ZoneId EARLIEST_ON_EARTH = ZoneId.of("Pacific/Kiritimati");

}
