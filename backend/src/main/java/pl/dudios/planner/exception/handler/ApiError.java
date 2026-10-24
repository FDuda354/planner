package pl.dudios.planner.exception.handler;

import java.time.LocalDateTime;

public record ApiError(
  String path,
  String msg,
  int status,
  LocalDateTime localDateTime
) {

}
