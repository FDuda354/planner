import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {RecurringTransactionService} from '../../../services/recurring-transaction.service';
import {RecurrenceFrequency, RecurringTransaction} from '../../../models/RecurringTransaction';
import {TransactionType} from '../../../models/Transaction';

export interface RecurringFilters {
  type: TransactionType | null;
  memberId: number | null;
  frequency: RecurrenceFrequency | null;
}

const NO_FILTERS: RecurringFilters = {type: null, memberId: null, frequency: null};

function matchesFilters(recurring: RecurringTransaction, filters: RecurringFilters): boolean {
  return (filters.type === null || recurring.type === filters.type)
         && (filters.memberId === null || recurring.memberId === filters.memberId)
         && (filters.frequency === null || recurring.frequency === filters.frequency);
}

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
  readonly filters = signal<RecurringFilters>(NO_FILTERS);
  readonly filteredRecurringTransactions = computed(() => this.recurringTransactions().filter(recurring => matchesFilters(recurring, this.filters())));
  readonly isFiltered = computed(() => Object.values(this.filters()).some(value => value !== null));

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

  countOfType(type: TransactionType | null): number {
    const filters = {...this.filters(), type};
    return this.recurringTransactions().filter(recurring => matchesFilters(recurring, filters)).length;
  }

  clearFilters(): void {
    this.filters.set(NO_FILTERS);
  }

  reload(): void {
    this.data.reload();
  }
}
