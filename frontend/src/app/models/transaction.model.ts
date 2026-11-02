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
  recurring: boolean;
}

export interface TransactionRequest {
  id: number | null;
  type: TransactionType;
  itemId: number;
  amount: number;
  date: string;
  memberId: number;
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

export interface SharedFlagCopy {
  title: string;
  hint: string;
}

const SHARED_FLAG_COPY: Record<TransactionType, SharedFlagCopy | null> = {
  [TransactionType.EXPENSE]: {
    title: 'Na dobro wspólne',
    hint: 'Np. rachunki, czynsz, wspólne zakupy — nie liczy się do niczyich osobistych wydatków',
  },
  [TransactionType.INCOME]: {
    title: 'Wspólny wpływ',
    hint: 'Np. prezent, zwrot podatku — nie przypisujemy go nikomu',
  },
  [TransactionType.SAVING]: null,
};

export function sharedFlagCopyOf(type: TransactionType): SharedFlagCopy | null {
  return SHARED_FLAG_COPY[type];
}

export function canBeShared(type: TransactionType): boolean {
  return SHARED_FLAG_COPY[type] !== null;
}

export function isSharedByDefault(type: TransactionType): boolean {
  return type === TransactionType.EXPENSE;
}

export function belongsToNobody(transaction: {type: TransactionType; shared: boolean}): boolean {
  return transaction.type === TransactionType.INCOME && transaction.shared;
}

export interface TransactionTypeFilterOption {
  type: TransactionType | null;
  label: string;
}

export const TRANSACTION_TYPE_FILTER_OPTIONS: TransactionTypeFilterOption[] = [
  {type: null, label: 'Wszystkie'},
  {type: TransactionType.EXPENSE, label: 'Wydatki'},
  {type: TransactionType.INCOME, label: 'Wpływy'},
  {type: TransactionType.SAVING, label: 'Oszczędności'},
];

const AMOUNT_SIGNS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: '−',
  [TransactionType.INCOME]: '+',
  [TransactionType.SAVING]: '',
};

export function amountSignOf(type: TransactionType): string {
  return AMOUNT_SIGNS[type];
}

const ITEM_LABELS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: 'Na co',
  [TransactionType.INCOME]: 'Skąd',
  [TransactionType.SAVING]: 'Na jaki cel',
};

export function itemLabelOf(type: TransactionType): string {
  return ITEM_LABELS[type];
}
