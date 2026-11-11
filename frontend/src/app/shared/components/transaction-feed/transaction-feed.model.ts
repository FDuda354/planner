import {Transaction, TransactionType} from '../../../models/transaction.model';
import {RecurringTransaction} from '../../../models/recurring-transaction.model';
import {BudgetItem} from '../../../models/budget-item.model';
import {TransactionFilters} from '../../utils/transaction-filters';

export interface ItemFilter {
  type: TransactionType;
  itemId: number;
}

export interface FeedFilters extends TransactionFilters {
  item: ItemFilter | null;
}

export interface DayGroup {
  date: string;
  label: string;
  expenses: number;
  transactions: Transaction[];
  recurring: RecurringTransaction[];
}

export interface ItemFilterOption {
  label: string;
  value: ItemFilter;
  item: BudgetItem;
}

export interface ItemFilterGroup {
  label: string;
  items: ItemFilterOption[];
}
