import {Injectable, signal} from '@angular/core';
import {Transaction, TransactionType} from '../models/Transaction';

export interface TransactionDraft {
  type: TransactionType;
  itemId: number | null;
}

export interface TransactionEditorState {
  visible: boolean;
  draft: TransactionDraft;
  editing: Transaction | null;
}

const NEW_EXPENSE: TransactionDraft = {type: TransactionType.EXPENSE, itemId: null};

@Injectable({providedIn: 'root'})
export class TransactionEditorStore {
  private readonly _state = signal<TransactionEditorState>({visible: false, draft: NEW_EXPENSE, editing: null});
  private readonly _browsedMonth = signal<string | null>(null);
  readonly state = this._state.asReadonly();
  readonly browsedMonth = this._browsedMonth.asReadonly();

  create(draft: TransactionDraft = NEW_EXPENSE): void {
    this._state.set({visible: true, draft, editing: null});
  }

  edit(transaction: Transaction): void {
    this._state.set({visible: true, draft: {type: transaction.type, itemId: transaction.itemId}, editing: transaction});
  }

  close(): void {
    this._state.update(state => ({...state, visible: false}));
  }

  browseMonth(month: string | null): void {
    this._browsedMonth.set(month);
  }
}
