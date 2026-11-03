import {computed, inject, Injectable, linkedSignal} from '@angular/core';
import {addDays, format, isSameDay, subDays} from 'date-fns';
import {pl} from 'date-fns/locale';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {TransactionType} from '../../../models/transaction.model';
import {parseDateKey} from '../../../shared/utils/month';
import {sumAmounts} from '../../../shared/utils/money';
import {
  FilterableTransaction, hasTransactionFilters, matchesTransactionFilters, NO_TRANSACTION_FILTERS, TransactionFilters, withSharedOnly, withType
} from '../../../shared/utils/transaction-filters';
import {DayGroup, FeedSection} from '../models/month-view';
import {MonthStore} from './month.store';
import {MonthForecastStore} from './month-forecast.store';

export interface FeedFilters extends TransactionFilters {
  categoryId: number | null;
}

const NO_FILTERS: FeedFilters = {...NO_TRANSACTION_FILTERS, categoryId: null};

@Injectable()
export class TransactionFeedStore {
  private readonly monthStore = inject(MonthStore);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly calendar = inject(CalendarStore);
  private readonly forecast = inject(MonthForecastStore);

  private readonly _filters = linkedSignal<string, FeedFilters>({source: this.monthStore.month, computation: () => NO_FILTERS});
  readonly filters = this._filters.asReadonly();
  readonly isFiltered = computed(() => hasTransactionFilters(this.filters()) || this.filters().categoryId !== null);

  readonly filteredTransactions = computed(() => this.monthStore.transactions().filter(t => this.matchesFilters(t, this.filters())));
  readonly filteredRecurring = computed(() => this.forecast.upcomingRecurring().filter(r => this.matchesFilters(r, this.filters())));
  readonly entryCount = computed(() => this.monthStore.transactions().length + this.forecast.upcomingRecurring().length);
  readonly filteredEntryCount = computed(() => this.filteredTransactions().length + this.filteredRecurring().length);
  readonly hasEntries = computed(() => this.entryCount() > 0);
  readonly dayGroups = computed<DayGroup[]>(() => {
    const dates = new Set([...this.filteredTransactions().map(t => t.date), ...this.filteredRecurring().map(r => r.nextOccurrence)]);
    return [...dates].sort((a, b) => b.localeCompare(a)).map(date => this.dayGroupOf(date));
  });
  readonly sections = computed<FeedSection[]>(() => [
    {planned: true, days: this.dayGroups().filter(day => this.monthStore.isPlanned(day.date))},
    {planned: false, days: this.dayGroups().filter(day => !this.monthStore.isPlanned(day.date))},
  ].filter(section => section.days.length > 0));

  countOfType(type: TransactionType | null): number {
    const filters = {...this.filters(), type};
    return [...this.monthStore.transactions(), ...this.forecast.upcomingRecurring()].filter(entry => this.matchesFilters(entry, filters)).length;
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

  private dayGroupOf(date: string): DayGroup {
    const transactions = this.filteredTransactions().filter(t => t.date === date);
    const recurring = this.filteredRecurring().filter(r => r.nextOccurrence === date);
    return {
      date,
      label: dayLabel(date, this.calendar.today()),
      expenses: sumAmounts([...transactions, ...recurring].filter(entry => entry.type === TransactionType.EXPENSE)),
      transactions,
      recurring,
    };
  }

  private matchesFilters(entry: FeedEntry, filters: FeedFilters): boolean {
    return matchesTransactionFilters(entry, filters, this.configStore.itemOf(entry.type, entry.itemId)?.name)
           && (filters.categoryId === null || (entry.type === TransactionType.EXPENSE && entry.itemId === filters.categoryId));
  }
}

type FeedEntry = FilterableTransaction & {itemId: number};

function dayLabel(dateKey: string, today: Date): string {
  const date = parseDateKey(dateKey);
  const fullDate = format(date, 'EEEE, d MMMM', {locale: pl});
  if (isSameDay(date, today)) return `Dziś · ${fullDate}`;
  if (isSameDay(date, addDays(today, 1))) return `Jutro · ${fullDate}`;
  if (isSameDay(date, subDays(today, 1))) return `Wczoraj · ${fullDate}`;
  return fullDate.charAt(0).toUpperCase() + fullDate.slice(1);
}
