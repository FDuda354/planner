import {belongsToNobody, canBeShared, TransactionType} from '../../models/transaction.model';
import {matchesSearch} from './search';

export interface TransactionFilters {
  type: TransactionType | null;
  memberId: number | null;
  shared: boolean | null;
  search: string;
}

export interface FilterableTransaction {
  type: TransactionType;
  memberId: number;
  shared: boolean;
  note: string;
  amount: number;
}

export const NO_TRANSACTION_FILTERS: TransactionFilters = {type: null, memberId: null, shared: null, search: ''};

export function withType<F extends TransactionFilters>(filters: F, type: TransactionType | null): F {
  return {...filters, type, shared: filters.shared === true && !allowsShared(type) ? null : filters.shared};
}

export function withShared<F extends TransactionFilters>(filters: F, shared: boolean | null): F {
  return {...filters, shared, type: shared === true && !allowsShared(filters.type) ? null : filters.type};
}

export function allowsShared(type: TransactionType | null): boolean {
  return type === null || canBeShared(type);
}

export function hasTransactionFilters(filters: TransactionFilters): boolean {
  return filters.type !== null || filters.memberId !== null || filters.shared !== null || filters.search.trim() !== '';
}

export function matchesTransactionFilters(transaction: FilterableTransaction, filters: TransactionFilters, itemName: string | undefined): boolean {
  return (filters.type === null || transaction.type === filters.type)
         && (filters.memberId === null || (transaction.memberId === filters.memberId && !belongsToNobody(transaction)))
         && (filters.shared === null || transaction.shared === filters.shared)
         && matchesSearch(filters.search, transaction, itemName);
}
