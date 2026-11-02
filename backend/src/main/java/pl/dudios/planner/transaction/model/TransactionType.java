package pl.dudios.planner.transaction.model;

public enum TransactionType {
  EXPENSE,
  INCOME,
  SAVING;

  public boolean canBeShared() {
    return this != SAVING;
  }
}
