import {max, min, required, schema} from '@angular/forms/signals';
import {TransactionType} from '../../../../models/Transaction';
import {RecurringTransaction, RecurringTransactionRequest} from '../../../../models/RecurringTransaction';

export interface RecurringFormModel {
  id: number | null;
  type: TransactionType;
  itemId: number | null;
  amount: number | null;
  dayOfMonth: number | null;
  memberId: number | null;
  shared: boolean;
  note: string;
  active: boolean;
}

export function newRecurringModel(memberId: number | null): RecurringFormModel {
  return {id: null, type: TransactionType.EXPENSE, itemId: null, amount: null, dayOfMonth: 1, memberId, shared: true, note: '', active: true};
}

export function editRecurringModel(recurring: RecurringTransaction): RecurringFormModel {
  return {
    id: recurring.id,
    type: recurring.type,
    itemId: recurring.itemId,
    amount: recurring.amount,
    dayOfMonth: recurring.dayOfMonth,
    memberId: recurring.memberId,
    shared: recurring.shared,
    note: recurring.note,
    active: recurring.active,
  };
}

export function toRecurringRequest(model: RecurringFormModel): RecurringTransactionRequest {
  return {
    id: model.id,
    type: model.type,
    itemId: model.itemId!,
    amount: model.amount!,
    dayOfMonth: model.dayOfMonth!,
    memberId: model.memberId!,
    shared: model.type === TransactionType.EXPENSE && model.shared,
    note: model.note.trim(),
    active: model.active,
  };
}

export const recurringSchema = schema<RecurringFormModel>((path) => {
  required(path.amount, {message: 'Podaj kwotę'});
  min(path.amount, 0.01, {message: 'Kwota musi być większa od zera'});
  required(path.itemId, {message: 'Wybierz pozycję'});
  required(path.dayOfMonth, {message: 'Podaj dzień miesiąca'});
  min(path.dayOfMonth, 1, {message: 'Dzień miesiąca musi być z zakresu 1–31'});
  max(path.dayOfMonth, 31, {message: 'Dzień miesiąca musi być z zakresu 1–31'});
  required(path.memberId, {message: 'Wybierz, czyja to transakcja'});
});
