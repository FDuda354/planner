import {min, required, schema} from '@angular/forms/signals';
import {Transaction, TransactionRequest, TransactionType} from '../../../../models/Transaction';
import {TransactionDraft} from '../../../../stores/transaction-editor.store';
import {parseDateKey, toDateKey} from '../../../utils/month';

export interface TransactionFormModel {
  id: number | null;
  type: TransactionType;
  itemId: number | null;
  amount: number | null;
  date: Date;
  shared: boolean;
  note: string;
}

export function newTransactionModel(draft: TransactionDraft, date: Date): TransactionFormModel {
  return {id: null, type: draft.type, itemId: draft.itemId, amount: null, date, shared: true, note: ''};
}

export function editTransactionModel(transaction: Transaction): TransactionFormModel {
  return {
    id: transaction.id,
    type: transaction.type,
    itemId: transaction.itemId,
    amount: transaction.amount,
    date: parseDateKey(transaction.date),
    shared: transaction.shared,
    note: transaction.note,
  };
}

export function toTransactionRequest(model: TransactionFormModel): TransactionRequest {
  return {
    id: model.id,
    type: model.type,
    itemId: model.itemId!,
    amount: model.amount!,
    date: toDateKey(model.date),
    shared: model.type === TransactionType.EXPENSE && model.shared,
    note: model.note.trim(),
  };
}

export const transactionSchema = schema<TransactionFormModel>((path) => {
  required(path.amount, {message: 'Podaj kwotę'});
  min(path.amount, 0.01, {message: 'Kwota musi być większa od zera'});
  required(path.itemId, {message: 'Wybierz kategorię'});
  required(path.date, {message: 'Wybierz datę'});
});
