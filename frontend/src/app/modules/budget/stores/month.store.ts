import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {forkJoin} from 'rxjs';
import {TransactionService} from '../../../services/transaction.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {HouseholdStore} from '../../../stores/household.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {netAmountOf, Transaction, TransactionType} from '../../../models/transaction.model';
import {daysInMonth, monthName, parseDateKey, shiftMonth} from '../../../shared/utils/month';
import {percentOf, sumAmounts} from '../../../shared/utils/money';
import {reloadWhenChanged} from '../../../shared/utils/resource';
import {CategoryShare, MemberShare, MonthTransactions} from '../models/month-view';

const EMPTY_MONTH: MonthTransactions = {current: [], previous: []};

@Injectable()
export class MonthStore {
  private readonly transactionService = inject(TransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly householdStore = inject(HouseholdStore);
  private readonly calendar = inject(CalendarStore);

  private readonly _month = signal(this.calendar.currentMonth());
  readonly month = this._month.asReadonly();

  readonly data = rxResource<MonthTransactions, string>({
    params: () => this._month(),
    stream: ({params}) => forkJoin({
      current: this.transactionService.getTransactions(params),
      previous: this.transactionService.getTransactions(shiftMonth(params, -1)),
    }),
    defaultValue: EMPTY_MONTH,
  });


  readonly transactions = computed(() => this.data.value().current);
  readonly bookedTransactions = computed(() => this.transactions().filter(t => !this.calendar.isAfterToday(t.date)));
  readonly plannedTransactions = computed(() => this.transactions().filter(t => this.calendar.isAfterToday(t.date)));
  readonly isCurrentMonth = computed(() => this._month() === this.calendar.currentMonth());
  readonly previousMonthName = computed(() => monthName(shiftMonth(this._month(), -1)));
  readonly daysInMonth = computed(() => daysInMonth(this._month()));
  readonly elapsedDays = computed(() => this.isCurrentMonth() ? this.calendar.today().getDate() : this.daysInMonth());

  readonly income = computed(() => sumOf(this.bookedTransactions(), TransactionType.INCOME));
  readonly expenses = computed(() => sumOf(this.bookedTransactions(), TransactionType.EXPENSE));
  readonly savings = computed(() => sumOf(this.bookedTransactions(), TransactionType.SAVING));
  readonly balance = computed(() => this.income() - this.expenses() - this.savings());
  readonly plannedNet = computed(() => netAmountOf(this.plannedTransactions()));
  readonly averageDailyExpenses = computed(() => this.expenses() / this.elapsedDays());

  readonly cumulativeExpenses = computed(() => cumulativeByDay(this.bookedTransactions(), this.daysInMonth(), this.elapsedDays()));
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

  readonly biggestExpense = computed(() => this.bookedTransactions()
    .filter(t => t.type === TransactionType.EXPENSE)
    .reduce<Transaction | null>((biggest, t) => biggest === null || t.amount > biggest.amount ? t : biggest, null));

  readonly categoryShares = computed<CategoryShare[]>(() => {
    const expenses = this.bookedTransactions().filter(t => t.type === TransactionType.EXPENSE);
    const total = this.expenses();
    return this.configStore.expenseCategories()
      .map(item => {
        const ofCategory = expenses.filter(t => t.itemId === item.id);
        const amount = sumAmounts(ofCategory);
        return {item, amount, share: percentOf(amount, total), count: ofCategory.length};
      })
      .filter(share => share.count > 0)
      .sort((a, b) => b.amount - a.amount);
  });

  readonly sharedExpenses = computed(() => sumAmounts(this.bookedTransactions().filter(t => t.type === TransactionType.EXPENSE && t.shared)));

  readonly memberShares = computed(() => this.sharesByMember(TransactionType.EXPENSE));

  readonly sharedIncome = computed(() => sumAmounts(this.bookedTransactions().filter(t => t.type === TransactionType.INCOME && t.shared)));
  readonly memberIncomeShares = computed(() => this.sharesByMember(TransactionType.INCOME));

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.data);
  }

  setMonth(month: string): void {
    this._month.set(month);
  }

  private sharesByMember(type: TransactionType): MemberShare[] {
    const personal = this.bookedTransactions().filter(t => t.type === type && !t.shared);
    const total = sumAmounts(personal);
    return this.householdStore.everyone()
      .map(member => {
        const amount = sumAmounts(personal.filter(t => t.memberId === member.id));
        return {member, amount, share: percentOf(amount, total)};
      })
      .filter(share => this.householdStore.isCurrentMember(share.member.id) || share.amount > 0);
  }
}

function sumOf(transactions: Transaction[], type: TransactionType): number {
  return sumAmounts(transactions.filter(t => t.type === type));
}

function cumulativeByDay(transactions: Transaction[], days: number, elapsedDays: number): number[] {
  const daily = new Array<number>(days).fill(0);
  transactions
    .filter(t => t.type === TransactionType.EXPENSE)
    .forEach(t => daily[parseDateKey(t.date).getDate() - 1] += t.amount);
  let total = 0;
  return daily.slice(0, elapsedDays).map(amount => total += amount);
}
