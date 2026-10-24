import {Injectable, signal} from '@angular/core';
import {BudgetItem} from '../../../models/BudgetItem';
import {TransactionType} from '../../../models/Transaction';

export interface BudgetItemEditorState {
  visible: boolean;
  kind: TransactionType;
  item: BudgetItem | null;
}

@Injectable()
export class BudgetItemEditorStore {
  private readonly _state = signal<BudgetItemEditorState>({visible: false, kind: TransactionType.EXPENSE, item: null});
  readonly state = this._state.asReadonly();

  create(kind: TransactionType): void {
    this._state.set({visible: true, kind, item: null});
  }

  edit(kind: TransactionType, item: BudgetItem): void {
    this._state.set({visible: true, kind, item});
  }

  close(): void {
    this._state.update(state => ({...state, visible: false}));
  }
}
