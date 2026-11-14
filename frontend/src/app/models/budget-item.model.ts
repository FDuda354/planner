export interface BudgetItem {
  id: number;
  name: string;
  icon: string;
  color: string;
}

export type ExpenseCategory = BudgetItem;

export interface IncomeSource extends BudgetItem {
  extra: boolean;
}

export interface SavingsGoal extends BudgetItem {
  note: string;
  initialAmount: number;
  targetAmount: number;
  ownerId: number | null;
}

export interface BudgetConfig {
  expenseCategories: ExpenseCategory[];
  incomeSources: IncomeSource[];
  savingsGoals: SavingsGoal[];
}

export interface ItemUsage {
  transactionCount: number;
  totalAmount: number;
}

export interface BudgetItemRequest {
  id: number | null;
  name: string;
  icon: string;
  color: string;
}

export interface IncomeSourceRequest extends BudgetItemRequest {
  extra: boolean;
}

export interface SavingsGoalRequest extends BudgetItemRequest {
  note: string;
  initialAmount: number;
  targetAmount: number;
  ownerId: number | null;
}

export const BUDGET_COLORS: string[] = [
  '#e11d48', '#f43f5e', '#ec4899', '#d946ef', '#a855f7', '#8b5cf6', '#6366f1', '#3b82f6',
  '#0ea5e9', '#06b6d4', '#14b8a6', '#10b981', '#22c55e', '#84cc16', '#eab308', '#f59e0b',
  '#f97316', '#ef4444', '#78716c', '#64748b',
];
