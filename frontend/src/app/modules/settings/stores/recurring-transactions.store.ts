import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {RecurringTransactionService} from '../../../services/recurring-transaction.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {RecurrenceFrequency, RecurringTransaction} from '../../../models/recurring-transaction.model';
import {TransactionType} from '../../../models/transaction.model';
import {
  hasTransactionFilters, matchesTransactionFilters, NO_TRANSACTION_FILTERS, TransactionFilters, withShared, withType
} from '../../../shared/utils/transaction-filters';

export interface RecurringFilters extends TransactionFilters {
  frequency: RecurrenceFrequency | null;
}

const NO_FILTERS: RecurringFilters = {...NO_TRANSACTION_FILTERS, frequency: null};

export interface RecurringEditorState {
  visible: boolean;
  recurring: RecurringTransaction | null;
}

@Injectable()
export class RecurringTransactionsStore {
  private readonly recurringService = inject(RecurringTransactionService);
  private readonly configStore = inject(BudgetConfigStore);

  readonly data = rxResource<RecurringTransaction[], void>({
    stream: () => this.recurringService.getRecurringTransactions(),
    defaultValue: [],
  });
  readonly recurringTransactions = computed(() => this.data.value());

  private readonly _filters = signal<RecurringFilters>(NO_FILTERS);
  readonly filters = this._filters.asReadonly();
  readonly filteredRecurringTransactions = computed(() => this.recurringTransactions().filter(recurring => this.matchesFilters(recurring, this.filters())));
  readonly isFiltered = computed(() => hasTransactionFilters(this.filters()) || this.filters().frequency !== null);

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
    return this.recurringTransactions().filter(recurring => this.matchesFilters(recurring, filters)).length;
  }

  selectType(type: TransactionType | null): void {
    this._filters.update(filters => withType(filters, type));
  }

  selectShared(shared: boolean | null): void {
    this._filters.update(filters => withShared(filters, shared));
  }

  selectMember(memberId: number | null): void {
    this._filters.update(filters => ({...filters, memberId}));
  }

  selectFrequency(frequency: RecurrenceFrequency | null): void {
    this._filters.update(filters => ({...filters, frequency}));
  }

  setSearch(search: string): void {
    this._filters.update(filters => ({...filters, search}));
  }

  clearFilters(): void {
    this._filters.set(NO_FILTERS);
  }

  reload(): void {
    this.data.reload();
  }

  private matchesFilters(recurring: RecurringTransaction, filters: RecurringFilters): boolean {
    return matchesTransactionFilters(recurring, filters, this.configStore.itemOf(recurring.type, recurring.itemId)?.name)
           && (filters.frequency === null || recurring.frequency === filters.frequency);
  }
}
