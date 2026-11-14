import {min, required, schema} from '@angular/forms/signals';
import {BUDGET_COLORS, BudgetItem, BudgetItemRequest, IncomeSource, IncomeSourceRequest, SavingsGoal, SavingsGoalRequest} from '../../../../models/budget-item.model';
import {DEFAULT_BUDGET_ICON} from '../../../../models/budget-icon.model';

export interface BudgetItemFormModel {
  id: number | null;
  name: string;
  icon: string;
  color: string;
  note: string;
  initialAmount: number | null;
  targetAmount: number | null;
  ownerId: number | null;
  extra: boolean;
}

export function newBudgetItemModel(): BudgetItemFormModel {
  return {
    id: null, name: '', icon: DEFAULT_BUDGET_ICON, color: BUDGET_COLORS[Math.floor(Math.random() * BUDGET_COLORS.length)],
    note: '', initialAmount: 0, targetAmount: null, ownerId: null, extra: false,
  };
}

export function editBudgetItemModel(item: BudgetItem): BudgetItemFormModel {
  const goal = item as Partial<SavingsGoal>;
  const source = item as Partial<IncomeSource>;
  return {
    id: item.id, name: item.name, icon: item.icon, color: item.color,
    note: goal.note ?? '', initialAmount: goal.initialAmount ?? 0, targetAmount: goal.targetAmount ?? null, ownerId: goal.ownerId ?? null,
    extra: source.extra ?? false,
  };
}

export function toBudgetItemRequest(model: BudgetItemFormModel): BudgetItemRequest {
  return {id: model.id, name: model.name.trim(), icon: model.icon, color: model.color};
}

export function toIncomeSourceRequest(model: BudgetItemFormModel): IncomeSourceRequest {
  return {...toBudgetItemRequest(model), extra: model.extra};
}

export function toSavingsGoalRequest(model: BudgetItemFormModel): SavingsGoalRequest {
  return {
    ...toBudgetItemRequest(model),
    note: model.note.trim(),
    initialAmount: model.initialAmount ?? 0,
    targetAmount: model.targetAmount!,
    ownerId: model.ownerId,
  };
}

export const budgetItemSchema = schema<BudgetItemFormModel>((path) => {
  required(path.name, {message: 'Podaj nazwę'});
});

export const savingsGoalSchema = schema<BudgetItemFormModel>((path) => {
  required(path.targetAmount, {message: 'Podaj kwotę celu'});
  min(path.targetAmount, 1, {message: 'Cel musi być większy od zera'});
  min(path.initialAmount, 0, {message: 'Kwota nie może być ujemna'});
});
