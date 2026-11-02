import {TransactionType} from './transaction.model';

export enum RecurrenceFrequency {
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
}

export interface RecurrenceFrequencyOption {
  frequency: RecurrenceFrequency;
  label: string;
}

export const RECURRENCE_FREQUENCY_OPTIONS: RecurrenceFrequencyOption[] = [
  {frequency: RecurrenceFrequency.MONTHLY, label: 'Co miesiąc'},
  {frequency: RecurrenceFrequency.YEARLY, label: 'Co rok'},
];

export interface RecurringTransaction {
  id: number;
  type: TransactionType;
  itemId: number;
  amount: number;
  frequency: RecurrenceFrequency;
  dayOfMonth: number;
  monthOfYear: number | null;
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
  frequency: RecurrenceFrequency;
  dayOfMonth: number;
  monthOfYear: number | null;
  memberId: number;
  shared: boolean;
  note: string;
  active: boolean;
}
