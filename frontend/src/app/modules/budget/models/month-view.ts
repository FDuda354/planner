import {BudgetItem} from '../../../models/budget-item.model';
import {Member} from '../../../models/member.model';
import {Transaction} from '../../../models/transaction.model';
import {RecurringTransaction} from '../../../models/recurring-transaction.model';

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
  recurring: RecurringTransaction[];
}

export interface FeedSection {
  planned: boolean;
  days: DayGroup[];
}
