import {computed, inject, Injectable, linkedSignal} from '@angular/core';
import {format, isSameDay, subDays} from 'date-fns';
import {pl} from 'date-fns/locale';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {Transaction, TransactionType} from '../../../models/transaction.model';
import {parseDateKey} from '../../../shared/utils/month';
import {sumAmounts} from '../../../shared/utils/money';
import {
  hasTransactionFilters, matchesTransactionFilters, NO_TRANSACTION_FILTERS, TransactionFilters, withSharedOnly, withType
} from '../../../shared/utils/transaction-filters';
import {DayGroup} from '../models/month-view';
import {MonthStore} from './month.store';

export interface FeedFilters extends TransactionFilters {
  categoryId: number | null;
}

const NO_FILTERS: FeedFilters = {...NO_TRANSACTION_FILTERS, categoryId: null};

@Injectable()
export class TransactionFeedStore {
  private readonly monthStore = inject(MonthStore);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly calendar = inject(CalendarStore);

  private readonly _filters = linkedSignal<string, FeedFilters>({source: this.monthStore.month, computation: () => NO_FILTERS});
  readonly filters = this._filters.asReadonly();
  readonly isFiltered = computed(() => hasTransactionFilters(this.filters()) || this.filters().categoryId !== null);

  readonly filteredTransactions = computed(() => this.monthStore.transactions().filter(t => this.matchesFilters(t, this.filters())));
  readonly dayGroups = computed<DayGroup[]>(() => {
    const groups = new Map<string, Transaction[]>();
    this.filteredTransactions().forEach(t => groups.set(t.date, [...(groups.get(t.date) ?? []), t]));
    return [...groups.entries()].map(([date, transactions]) => ({
      date,
      label: dayLabel(date, this.calendar.today()),
      expenses: sumAmounts(transactions.filter(t => t.type === TransactionType.EXPENSE)),
      transactions,
    }));
  });

  countOfType(type: TransactionType | null): number {
    const filters = {...this.filters(), type};
    return this.monthStore.transactions().filter(t => this.matchesFilters(t, filters)).length;
  }

  selectType(type: TransactionType | null): void {
    this._filters.update(filters => ({...withType(filters, type), categoryId: type === null || type === TransactionType.EXPENSE ? filters.categoryId : null}));
  }

  selectSharedOnly(sharedOnly: boolean): void {
    this._filters.update(filters => withSharedOnly(filters, sharedOnly));
  }

  selectMember(memberId: number | null): void {
    this._filters.update(filters => ({...filters, memberId}));
  }

  toggleCategory(categoryId: number): void {
    this._filters.update(filters => filters.categoryId === categoryId
      ? {...filters, categoryId: null}
      : {...withType(filters, TransactionType.EXPENSE), categoryId});
  }

  setSearch(search: string): void {
    this._filters.update(filters => ({...filters, search}));
  }

  clearFilters(): void {
    this._filters.set(NO_FILTERS);
  }

  private matchesFilters(transaction: Transaction, filters: FeedFilters): boolean {
    return matchesTransactionFilters(transaction, filters, this.configStore.itemOf(transaction.type, transaction.itemId)?.name)
           && (filters.categoryId === null || (transaction.type === TransactionType.EXPENSE && transaction.itemId === filters.categoryId));
  }
}

function dayLabel(dateKey: string, today: Date): string {
  const date = parseDateKey(dateKey);
  const fullDate = format(date, 'EEEE, d MMMM', {locale: pl});
  if (isSameDay(date, today)) return `Dziś · ${fullDate}`;
  if (isSameDay(date, subDays(today, 1))) return `Wczoraj · ${fullDate}`;
  return fullDate.charAt(0).toUpperCase() + fullDate.slice(1);
}
