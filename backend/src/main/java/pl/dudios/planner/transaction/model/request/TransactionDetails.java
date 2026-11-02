package pl.dudios.planner.transaction.model.request;

import pl.dudios.planner.transaction.model.TransactionType;

public interface TransactionDetails {

  TransactionType type();

  Long itemId();

  Long memberId();

  boolean shared();

}
