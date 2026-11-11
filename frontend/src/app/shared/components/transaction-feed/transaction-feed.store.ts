import {computed, inject, Injectable, linkedSignal} from '@angular/core';
import {addDays, format, isSameDay, subDays} from 'date-fns';
import {pl} from 'date-fns/locale';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {Transaction, TransactionType} from '../../../models/transaction.model';
import {RecurringTransaction} from '../../../models/recurring-transaction.model';
import {parseDateKey} from '../../utils/month';
import {sumAmounts} from '../../utils/money';
import {
  allowsShared, FilterableTransaction, hasTransactionFilters, matchesTransactionFilters, NO_TRANSACTION_FILTERS, withShared, withType
} from '../../utils/transaction-filters';
import {DayGroup, FeedFilters, ItemFilter} from './transaction-feed.model';
import {TransactionFeedSource} from './transaction-feed-source';

export const FEED_PAGE_SIZE = 25;

const NO_FILTERS: FeedFilters = {...NO_TRANSACTION_FILTERS, item: null};

@Injectable()
export class TransactionFeedStore {
  private readonly source = inject(TransactionFeedSource);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly calendar = inject(CalendarStore);

  private readonly _filters = linkedSignal<string, FeedFilters>({source: this.source.period, computation: () => NO_FILTERS});
  readonly filters = this._filters.asReadonly();
  readonly isFiltered = computed(() => hasTransactionFilters(this.filters()) || this.filters().item !== null);
  readonly selectedCategoryId = computed(() => {
    const item = this.filters().item;
    return item?.type === TransactionType.EXPENSE ? item.itemId : null;
  });

  readonly isFirstLoad = computed(() => this.source.isLoading() && this.source.transactions().length === 0);
  readonly entryCount = computed(() => this.source.transactions().length + this.source.upcomingRecurring().length);
  readonly hasEntries = computed(() => this.entryCount() > 0);

  private readonly filteredTransactions = computed(() => this.source.transactions().filter(t => this.matchesFilters(t, this.filters())));
  private readonly filteredRecurring = computed(() => this.source.upcomingRecurring().filter(r => this.matchesFilters(r, this.filters())));
  readonly filteredEntryCount = computed(() => this.filteredTransactions().length + this.filteredRecurring().length);
  readonly hasMatches = computed(() => this.filteredEntryCount() > 0);

  private readonly plannedTransactions = computed(() => this.filteredTransactions().filter(t => this.calendar.isAfterToday(t.date)));
  private readonly bookedTransactions = computed(() => this.filteredTransactions().filter(t => !this.calendar.isAfterToday(t.date)));

  private readonly requestedFirst = linkedSignal({
    source: () => ({period: this.source.period(), filters: this.filters()}),
    computation: () => 0,
  });
  readonly bookedCount = computed(() => this.bookedTransactions().length);
  readonly isPaginated = computed(() => this.bookedCount() > FEED_PAGE_SIZE);
  readonly first = computed(() => Math.min(this.requestedFirst(), lastPageFirst(this.bookedCount())));
  readonly bookedDays = computed(() => this.dayGroupsOf(this.bookedTransactions().slice(this.first(), this.first() + FEED_PAGE_SIZE), []));

  readonly plannedCount = computed(() => this.plannedTransactions().length + this.filteredRecurring().length);
  readonly showsPlanned = computed(() => this.first() === 0 && this.plannedCount() > 0);
  readonly plannedDays = computed(() => this.dayGroupsOf(this.plannedTransactions(), this.filteredRecurring()));

  countOfType(type: TransactionType | null): number {
    const filters = {...this.filters(), type};
    return [...this.source.transactions(), ...this.source.upcomingRecurring()].filter(entry => this.matchesFilters(entry, filters)).length;
  }

  selectType(type: TransactionType | null): void {
    this._filters.update(filters => ({...withType(filters, type), item: type === null || filters.item?.type === type ? filters.item : null}));
  }

  selectShared(shared: boolean | null): void {
    this._filters.update(filters => ({...withShared(filters, shared), item: isItemAllowedWithShared(filters.item, shared) ? filters.item : null}));
  }

  selectMember(memberId: number | null): void {
    this._filters.update(filters => ({...filters, memberId}));
  }

  selectItem(item: ItemFilter | null): void {
    this._filters.update(filters => item === null ? {...filters, item} : {...withType(filters, item.type), item});
  }

  toggleCategory(categoryId: number): void {
    this.selectItem(this.selectedCategoryId() === categoryId ? null : {type: TransactionType.EXPENSE, itemId: categoryId});
  }

  setSearch(search: string): void {
    this._filters.update(filters => ({...filters, search}));
  }

  clearFilters(): void {
    this._filters.set(NO_FILTERS);
  }

  showPage(first: number): void {
    this.requestedFirst.set(first);
  }

  private dayGroupsOf(transactions: Transaction[], recurring: RecurringTransaction[]): DayGroup[] {
    const dates = new Set([...transactions.map(t => t.date), ...recurring.map(r => r.nextOccurrence)]);
    return [...dates].sort((a, b) => b.localeCompare(a)).map(date => ({
      date,
      label: dayLabel(date, this.calendar.today()),
      expenses: this.expensesOn(date),
      transactions: transactions.filter(t => t.date === date),
      recurring: recurring.filter(r => r.nextOccurrence === date),
    }));
  }

  private expensesOn(date: string): number {
    const transactions = this.filteredTransactions().filter(t => t.date === date);
    const recurring = this.filteredRecurring().filter(r => r.nextOccurrence === date);
    return sumAmounts([...transactions, ...recurring].filter(entry => entry.type === TransactionType.EXPENSE));
  }

  private matchesFilters(entry: FeedEntry, filters: FeedFilters): boolean {
    return matchesTransactionFilters(entry, filters, this.configStore.itemOf(entry.type, entry.itemId)?.name)
           && (filters.item === null || (entry.type === filters.item.type && entry.itemId === filters.item.itemId));
  }
}

type FeedEntry = FilterableTransaction & {itemId: number};

function isItemAllowedWithShared(item: ItemFilter | null, shared: boolean | null): boolean {
  return shared !== true || item === null || allowsShared(item.type);
}

function lastPageFirst(count: number): number {
  return Math.max(Math.ceil(count / FEED_PAGE_SIZE) - 1, 0) * FEED_PAGE_SIZE;
}

function dayLabel(dateKey: string, today: Date): string {
  const date = parseDateKey(dateKey);
  const fullDate = format(date, 'EEEE, d MMMM', {locale: pl});
  if (isSameDay(date, today)) return `Dziś · ${fullDate}`;
  if (isSameDay(date, addDays(today, 1))) return `Jutro · ${fullDate}`;
  if (isSameDay(date, subDays(today, 1))) return `Wczoraj · ${fullDate}`;
  return fullDate.charAt(0).toUpperCase() + fullDate.slice(1);
}
