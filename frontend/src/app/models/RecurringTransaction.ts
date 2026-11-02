import {TransactionType} from './Transaction';

export interface RecurringTransaction {
  id: number;
  type: TransactionType;
  itemId: number;
  amount: number;
  dayOfMonth: number;
  memberId: number;
  shared: boolean;
  note: string;
  active: boolean;
  nextOccurrence: string;
}

export interface RecurringTransactionRequest {
  id: number | null;
  type: TransactionType;
  itemId: number;
  amount: number;
  dayOfMonth: number;
  memberId: number;
  shared: boolean;
  note: string;
  active: boolean;
}
