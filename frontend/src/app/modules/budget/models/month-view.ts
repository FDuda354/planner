import {BudgetItem} from '../../../models/BudgetItem';
import {Member} from '../../../models/Member';
import {Transaction, TransactionType} from '../../../models/Transaction';

export interface MonthTransactions {
  current: Transaction[];
  previous: Transaction[];
}

export interface CategoryShare {
  item: BudgetItem;
  amount: number;
  share: number;
  count: number;
}

export interface MemberShare {
  member: Member;
  amount: number;
  share: number;
}

export interface DayGroup {
  date: string;
  label: string;
  expenses: number;
  transactions: Transaction[];
}

export const ALL_TYPES = 'ALL';

export type FeedFilter = TransactionType | typeof ALL_TYPES;
