export enum TransactionType {
  EXPENSE = 'EXPENSE',
  INCOME = 'INCOME',
  SAVING = 'SAVING',
}

export interface Transaction {
  id: number;
  type: TransactionType;
  itemId: number;
  amount: number;
  date: string;
  memberId: number;
  shared: boolean;
  note: string;
}

export interface TransactionRequest {
  id: number | null;
  type: TransactionType;
  itemId: number;
  amount: number;
  date: string;
  shared: boolean;
  note: string;
}

export interface TransactionTypeOption {
  type: TransactionType;
  label: string;
  verb: string;
  icon: string;
}

export const TRANSACTION_TYPE_OPTIONS: TransactionTypeOption[] = [
  {type: TransactionType.EXPENSE, label: 'Wydatek', verb: 'Dodaj wydatek', icon: 'pi pi-arrow-up-right'},
  {type: TransactionType.INCOME, label: 'Wpływ', verb: 'Dodaj wpływ', icon: 'pi pi-arrow-down-left'},
  {type: TransactionType.SAVING, label: 'Oszczędność', verb: 'Odłóż na cel', icon: 'pi pi-shield'},
];
