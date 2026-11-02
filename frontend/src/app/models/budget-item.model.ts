export interface BudgetItem {
  id: number;
  name: string;
  icon: string;
  color: string;
}

export type ExpenseCategory = BudgetItem;

export type IncomeSource = BudgetItem;

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

export interface SavingsGoalRequest extends BudgetItemRequest {
  note: string;
  initialAmount: number;
  targetAmount: number;
  ownerId: number | null;
}

export const BUDGET_ICONS: string[] = [
  'pi-home', 'pi-shopping-cart', 'pi-shop', 'pi-car', 'pi-bolt', 'pi-wifi', 'pi-mobile', 'pi-heart',
  'pi-face-smile', 'pi-ticket', 'pi-video', 'pi-headphones', 'pi-book', 'pi-graduation-cap', 'pi-shopping-bag', 'pi-gift',
  'pi-wrench', 'pi-hammer', 'pi-globe', 'pi-sun', 'pi-camera', 'pi-palette', 'pi-truck', 'pi-receipt',
  'pi-briefcase', 'pi-building', 'pi-building-columns', 'pi-wallet', 'pi-money-bill', 'pi-credit-card', 'pi-trophy', 'pi-crown',
  'pi-shield', 'pi-bullseye', 'pi-sparkles', 'pi-lightbulb', 'pi-users', 'pi-user', 'pi-star', 'pi-box',
  'pi-cart-plus', 'pi-tag', 'pi-tags', 'pi-percentage', 'pi-calculator', 'pi-chart-line', 'pi-chart-pie', 'pi-chart-bar',
  'pi-dollar', 'pi-euro', 'pi-pound', 'pi-bitcoin', 'pi-ethereum', 'pi-paypal', 'pi-id-card', 'pi-key',
  'pi-lock', 'pi-warehouse', 'pi-map', 'pi-map-marker', 'pi-compass', 'pi-send', 'pi-phone', 'pi-envelope',
  'pi-desktop', 'pi-tablet', 'pi-microchip', 'pi-print', 'pi-image', 'pi-microphone', 'pi-volume-up', 'pi-moon',
  'pi-cloud', 'pi-calendar', 'pi-clock', 'pi-stopwatch', 'pi-hourglass', 'pi-flag', 'pi-bookmark', 'pi-thumbs-up',
  'pi-heart-fill', 'pi-star-fill', 'pi-pencil', 'pi-file', 'pi-clipboard', 'pi-inbox', 'pi-folder', 'pi-megaphone',
  'pi-wave-pulse', 'pi-verified', 'pi-stamp', 'pi-gauge', 'pi-cog', 'pi-database', 'pi-code', 'pi-eye',
  'pi-venus', 'pi-mars', 'pi-comments', 'pi-bell', 'pi-thumbtack', 'pi-note-sticky', 'pi-barcode', 'pi-power-off',
];

export const BUDGET_COLORS: string[] = [
  '#e11d48', '#f43f5e', '#ec4899', '#d946ef', '#a855f7', '#8b5cf6', '#6366f1', '#3b82f6',
  '#0ea5e9', '#06b6d4', '#14b8a6', '#10b981', '#22c55e', '#84cc16', '#eab308', '#f59e0b',
  '#f97316', '#ef4444', '#78716c', '#64748b',
];
