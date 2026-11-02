import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {RecurringTransactionService} from '../../../services/recurring-transaction.service';
import {RecurringTransaction} from '../../../models/RecurringTransaction';

export interface RecurringEditorState {
  visible: boolean;
  recurring: RecurringTransaction | null;
}

@Injectable()
export class RecurringTransactionsStore {
  private readonly recurringService = inject(RecurringTransactionService);

  readonly data = rxResource<RecurringTransaction[], void>({
    stream: () => this.recurringService.getRecurringTransactions(),
    defaultValue: [],
  });
  readonly recurringTransactions = computed(() => this.data.value());

  private readonly _editor = signal<RecurringEditorState>({visible: false, recurring: null});
  readonly editor = this._editor.asReadonly();

  create(): void {
    this._editor.set({visible: true, recurring: null});
  }

  edit(recurring: RecurringTransaction): void {
    this._editor.set({visible: true, recurring});
  }

  close(): void {
    this._editor.update(state => ({...state, visible: false}));
  }

  reload(): void {
    this.data.reload();
  }
}
