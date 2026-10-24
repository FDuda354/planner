import {SavingsGoal} from './BudgetItem';

export interface ItemAmount {
  itemId: number;
  amount: number;
}

export interface MemberAmount {
  memberId: number;
  amount: number;
}

export interface MonthSummary {
  month: string;
  income: number;
  expenses: number;
  savings: number;
  expensesByCategory: ItemAmount[];
  incomesBySource: ItemAmount[];
  savingsByGoal: ItemAmount[];
  personalExpensesByMember: MemberAmount[];
  sharedExpenses: number;
}

export interface MonthlyContribution {
  month: string;
  amount: number;
}

export interface GoalProgress {
  goal: SavingsGoal;
  saved: number;
  contributions: MonthlyContribution[];
}
