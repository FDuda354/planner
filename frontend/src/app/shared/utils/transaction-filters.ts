import {belongsToNobody, canBeShared, TransactionType} from '../../models/transaction.model';
import {matchesSearch} from './search';

export interface TransactionFilters {
  type: TransactionType | null;
  memberId: number | null;
  sharedOnly: boolean;
  search: string;
}

export interface FilterableTransaction {
  type: TransactionType;
  memberId: number;
  shared: boolean;
  note: string;
  amount: number;
}

export const NO_TRANSACTION_FILTERS: TransactionFilters = {type: null, memberId: null, sharedOnly: false, search: ''};

export function withType<F extends TransactionFilters>(filters: F, type: TransactionType | null): F {
  return {...filters, type, sharedOnly: (type === null || canBeShared(type)) && filters.sharedOnly};
}

export function withSharedOnly<F extends TransactionFilters>(filters: F, sharedOnly: boolean): F {
  const keepsType = filters.type === null || canBeShared(filters.type);
  return {...filters, sharedOnly, type: sharedOnly && !keepsType ? null : filters.type};
}

export function hasTransactionFilters(filters: TransactionFilters): boolean {
  return filters.type !== null || filters.memberId !== null || filters.sharedOnly || filters.search.trim() !== '';
}

export function matchesTransactionFilters(transaction: FilterableTransaction, filters: TransactionFilters, itemName: string | undefined): boolean {
  return (filters.type === null || transaction.type === filters.type)
         && (filters.memberId === null || (transaction.memberId === filters.memberId && !belongsToNobody(transaction)))
         && (!filters.sharedOnly || transaction.shared)
         && matchesSearch(filters.search, transaction, itemName);
}
