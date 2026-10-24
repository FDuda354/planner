import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {forkJoin} from 'rxjs';
import {format, isSameDay, subDays} from 'date-fns';
import {pl} from 'date-fns/locale';
import {TransactionService} from '../../../services/transaction.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {HouseholdStore} from '../../../stores/household.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {Transaction, TransactionType} from '../../../models/Transaction';
import {daysInMonth, monthName, parseDateKey, shiftMonth} from '../../../shared/utils/month';
import {percentOf} from '../../../shared/utils/money';
import {reloadWhenChanged} from '../../../shared/utils/resource';
import {ALL_TYPES, CategoryShare, DayGroup, FeedFilter, MemberShare, MonthTransactions} from '../models/month-view';

const EMPTY_MONTH: MonthTransactions = {current: [], previous: []};

@Injectable()
export class MonthStore {
  private readonly transactionService = inject(TransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly householdStore = inject(HouseholdStore);
  private readonly calendar = inject(CalendarStore);

  private readonly _month = signal(this.calendar.currentMonth());
  private readonly _feedFilter = signal<FeedFilter>(ALL_TYPES);
  private readonly _categoryFilter = signal<number | null>(null);
  private readonly _memberFilter = signal<number | null>(null);
  private readonly _search = signal('');

  readonly month = this._month.asReadonly();
  readonly feedFilter = this._feedFilter.asReadonly();
  readonly categoryFilter = this._categoryFilter.asReadonly();
  readonly memberFilter = this._memberFilter.asReadonly();
  readonly search = this._search.asReadonly();

  readonly data = rxResource<MonthTransactions, string>({
    params: () => this._month(),
    stream: ({params}) => forkJoin({
      current: this.transactionService.getTransactions(params),
      previous: this.transactionService.getTransactions(shiftMonth(params, -1)),
    }),
    defaultValue: EMPTY_MONTH,
  });

  readonly transactions = computed(() => this.data.value().current);
  readonly isCurrentMonth = computed(() => this._month() === this.calendar.currentMonth());
  readonly previousMonthName = computed(() => monthName(shiftMonth(this._month(), -1)));
  readonly daysInMonth = computed(() => daysInMonth(this._month()));
  readonly elapsedDays = computed(() => this.isCurrentMonth() ? this.calendar.today().getDate() : this.daysInMonth());

  readonly income = computed(() => sumOf(this.transactions(), TransactionType.INCOME));
  readonly expenses = computed(() => sumOf(this.transactions(), TransactionType.EXPENSE));
  readonly savings = computed(() => sumOf(this.transactions(), TransactionType.SAVING));
  readonly balance = computed(() => this.income() - this.expenses() - this.savings());
  readonly averageDailyExpenses = computed(() => this.expenses() / this.elapsedDays());

  readonly cumulativeExpenses = computed(() => cumulativeByDay(this.transactions(), this.daysInMonth(), this.elapsedDays()));
  readonly previousCumulativeExpenses = computed(() => {
    const days = daysInMonth(shiftMonth(this._month(), -1));
    return cumulativeByDay(this.data.value().previous, days, days);
  });

  readonly hasPreviousMonthExpenses = computed(() => this.data.value().previous.some(t => t.type === TransactionType.EXPENSE));

  readonly previousExpensesSoFar = computed(() => {
    const previous = this.previousCumulativeExpenses();
    return previous.length === 0 ? 0 : previous[Math.min(this.elapsedDays(), previous.length) - 1];
  });

  readonly expensesChange = computed(() => {
    const previous = this.previousExpensesSoFar();
    return previous === 0 ? null : (this.expenses() - previous) / previous;
  });

  readonly biggestExpense = computed(() => this.transactions()
    .filter(t => t.type === TransactionType.EXPENSE)
    .reduce<Transaction | null>((biggest, t) => biggest === null || t.amount > biggest.amount ? t : biggest, null));

  readonly categoryShares = computed<CategoryShare[]>(() => {
    const expenses = this.transactions().filter(t => t.type === TransactionType.EXPENSE);
    const total = this.expenses();
    return this.configStore.expenseCategories()
      .map(item => {
        const ofCategory = expenses.filter(t => t.itemId === item.id);
        const amount = ofCategory.reduce((sum, t) => sum + t.amount, 0);
        return {item, amount, share: percentOf(amount, total), count: ofCategory.length};
      })
      .filter(share => share.count > 0)
      .sort((a, b) => b.amount - a.amount);
  });

  readonly sharedExpenses = computed(() => this.transactions()
    .filter(t => t.type === TransactionType.EXPENSE && t.shared)
    .reduce((sum, t) => sum + t.amount, 0));
  readonly personalExpenses = computed(() => this.expenses() - this.sharedExpenses());

  readonly memberShares = computed<MemberShare[]>(() => {
    const personal = this.transactions().filter(t => t.type === TransactionType.EXPENSE && !t.shared);
    const total = this.personalExpenses();
    return this.householdStore.everyone()
      .map(member => {
        const amount = personal.filter(t => t.memberId === member.id).reduce((sum, t) => sum + t.amount, 0);
        return {member, amount, share: percentOf(amount, total)};
      })
      .filter(share => this.householdStore.isCurrentMember(share.member.id) || share.amount > 0);
  });

  readonly filteredTransactions = computed(() => this.transactions().filter(t => this.matchesFilters(t)));

  readonly dayGroups = computed<DayGroup[]>(() => {
    const groups = new Map<string, Transaction[]>();
    this.filteredTransactions().forEach(t => groups.set(t.date, [...(groups.get(t.date) ?? []), t]));
    return [...groups.entries()].map(([date, transactions]) => ({
      date,
      label: dayLabel(date, this.calendar.today()),
      expenses: sumOf(transactions, TransactionType.EXPENSE),
      transactions,
    }));
  });

  readonly hasActiveFilters = computed(() => this._feedFilter() !== ALL_TYPES || this._categoryFilter() !== null
    || this._memberFilter() !== null || this._search().trim() !== '');

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.data);
  }

  setMonth(month: string): void {
    this._month.set(month);
    this.clearFilters();
  }

  setFeedFilter(filter: FeedFilter): void {
    this._feedFilter.set(filter);
    if (filter !== ALL_TYPES && filter !== TransactionType.EXPENSE) {
      this._categoryFilter.set(null);
    }
  }

  toggleCategoryFilter(categoryId: number): void {
    const isSelected = this._categoryFilter() === categoryId;
    this._categoryFilter.set(isSelected ? null : categoryId);
    if (!isSelected) {
      this._feedFilter.set(TransactionType.EXPENSE);
    }
  }

  setMemberFilter(memberId: number | null): void {
    this._memberFilter.set(memberId);
  }

  setSearch(search: string): void {
    this._search.set(search);
  }

  clearFilters(): void {
    this._feedFilter.set(ALL_TYPES);
    this._categoryFilter.set(null);
    this._memberFilter.set(null);
    this._search.set('');
  }

  private matchesFilters(transaction: Transaction): boolean {
    const feedFilter = this._feedFilter();
    const categoryFilter = this._categoryFilter();
    const memberFilter = this._memberFilter();
    const search = this._search().trim().toLowerCase();
    return (feedFilter === ALL_TYPES || transaction.type === feedFilter)
      && (categoryFilter === null || (transaction.type === TransactionType.EXPENSE && transaction.itemId === categoryFilter))
      && (memberFilter === null || transaction.memberId === memberFilter)
      && (search === '' || this.searchableText(transaction).includes(search));
  }

  private searchableText(transaction: Transaction): string {
    const item = this.configStore.itemOf(transaction.type, transaction.itemId);
    const amount = transaction.amount.toFixed(2);
    return `${transaction.note} ${item?.name ?? ''} ${amount} ${amount.replace('.', ',')}`.toLowerCase();
  }
}

function sumOf(transactions: Transaction[], type: TransactionType): number {
  return transactions.filter(t => t.type === type).reduce((sum, t) => sum + t.amount, 0);
}

function cumulativeByDay(transactions: Transaction[], days: number, elapsedDays: number): number[] {
  const daily = new Array<number>(days).fill(0);
  transactions
    .filter(t => t.type === TransactionType.EXPENSE)
    .forEach(t => daily[parseDateKey(t.date).getDate() - 1] += t.amount);
  let total = 0;
  return daily.slice(0, elapsedDays).map(amount => total += amount);
}

function dayLabel(dateKey: string, today: Date): string {
  const date = parseDateKey(dateKey);
  const fullDate = format(date, 'EEEE, d MMMM', {locale: pl});
  if (isSameDay(date, today)) return `Dziś · ${fullDate}`;
  if (isSameDay(date, subDays(today, 1))) return `Wczoraj · ${fullDate}`;
  return fullDate.charAt(0).toUpperCase() + fullDate.slice(1);
}
