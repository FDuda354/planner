import {max, min, required, schema, validate} from '@angular/forms/signals';
import {canBeShared, TransactionType} from '../../../../models/transaction.model';
import {RecurrenceFrequency, RecurringTransaction, RecurringTransactionRequest} from '../../../../models/recurring-transaction.model';
import {maxDayOf, RecurrenceSchedule} from './recurring-schedule';

export interface RecurringFormModel {
  id: number | null;
  type: TransactionType;
  itemId: number | null;
  amount: number | null;
  frequency: RecurrenceFrequency;
  dayOfMonth: number | null;
  monthOfYear: number | null;
  memberId: number | null;
  shared: boolean;
  note: string;
  active: boolean;
}

export function newRecurringModel(memberId: number | null): RecurringFormModel {
  return {
    id: null,
    type: TransactionType.EXPENSE,
    itemId: null,
    amount: null,
    frequency: RecurrenceFrequency.MONTHLY,
    dayOfMonth: 1,
    monthOfYear: null,
    memberId,
    shared: true,
    note: '',
    active: true,
  };
}

export function editRecurringModel(recurring: RecurringTransaction): RecurringFormModel {
  return {
    id: recurring.id,
    type: recurring.type,
    itemId: recurring.itemId,
    amount: recurring.amount,
    frequency: recurring.frequency,
    dayOfMonth: recurring.dayOfMonth,
    monthOfYear: recurring.monthOfYear,
    memberId: recurring.memberId,
    shared: recurring.shared,
    note: recurring.note,
    active: recurring.active,
  };
}

export function scheduleOf(model: RecurringFormModel): RecurrenceSchedule | null {
  const isYearly = model.frequency === RecurrenceFrequency.YEARLY;
  if (model.dayOfMonth === null || (isYearly && model.monthOfYear === null) || model.dayOfMonth > maxDayOf(model.monthOfYear)) {
    return null;
  }
  return {frequency: model.frequency, dayOfMonth: model.dayOfMonth, monthOfYear: isYearly ? model.monthOfYear : null};
}

export function toRecurringRequest(model: RecurringFormModel): RecurringTransactionRequest {
  const schedule = scheduleOf(model)!;
  return {
    id: model.id,
    type: model.type,
    itemId: model.itemId!,
    amount: model.amount!,
    frequency: schedule.frequency,
    dayOfMonth: schedule.dayOfMonth,
    monthOfYear: schedule.monthOfYear,
    memberId: model.memberId!,
    shared: canBeShared(model.type) && model.shared,
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
  required(path.monthOfYear, {message: 'Wybierz miesiąc', when: ({valueOf}) => valueOf(path.frequency) === RecurrenceFrequency.YEARLY});
  validate(path.dayOfMonth, ({value, valueOf}) => {
    const isYearly = valueOf(path.frequency) === RecurrenceFrequency.YEARLY;
    const day = value();
    return isYearly && day !== null && day > maxDayOf(valueOf(path.monthOfYear))
      ? {kind: 'dayOutsideMonth', message: 'Ten miesiąc nie ma tylu dni'}
      : null;
  });
  required(path.memberId, {message: 'Wybierz, czyja to transakcja'});
});
