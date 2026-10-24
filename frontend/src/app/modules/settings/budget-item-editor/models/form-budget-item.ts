import {min, required, schema} from '@angular/forms/signals';
import {
  BUDGET_COLORS, BUDGET_ICONS, BudgetItem, BudgetItemRequest, SavingsGoal, SavingsGoalRequest
} from '../../../../models/BudgetItem';

export interface BudgetItemFormModel {
  id: number | null;
  name: string;
  icon: string;
  color: string;
  active: boolean;
  note: string;
  initialAmount: number | null;
  targetAmount: number | null;
  ownerId: number | null;
}

export function newBudgetItemModel(): BudgetItemFormModel {
  return {
    id: null, name: '', icon: BUDGET_ICONS[0], color: BUDGET_COLORS[Math.floor(Math.random() * BUDGET_COLORS.length)],
    active: true, note: '', initialAmount: 0, targetAmount: null, ownerId: null,
  };
}

export function editBudgetItemModel(item: BudgetItem): BudgetItemFormModel {
  const goal = item as Partial<SavingsGoal>;
  return {
    id: item.id, name: item.name, icon: item.icon, color: item.color, active: item.active,
    note: goal.note ?? '', initialAmount: goal.initialAmount ?? 0, targetAmount: goal.targetAmount ?? null, ownerId: goal.ownerId ?? null,
  };
}

export function toBudgetItemRequest(model: BudgetItemFormModel): BudgetItemRequest {
  return {id: model.id, name: model.name.trim(), icon: model.icon, color: model.color, active: model.active};
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
