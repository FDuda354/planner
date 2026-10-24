import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {SummaryService} from '../../../services/summary.service';
import {TransactionService} from '../../../services/transaction.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {HouseholdStore} from '../../../stores/household.store';
import {ItemAmount, MonthSummary} from '../../../models/Summary';
import {BudgetItem} from '../../../models/BudgetItem';
import {Member} from '../../../models/Member';
import {TransactionType} from '../../../models/Transaction';
import {yearOfMonth} from '../../../shared/utils/month';
import {CalendarStore} from '../../../stores/calendar.store';
import {percentOf} from '../../../shared/utils/money';
import {reloadWhenChanged} from '../../../shared/utils/resource';

export interface ItemYearTotal {
  item: BudgetItem;
  total: number;
  share: number;
  byMonth: number[];
}

export interface MemberYearTotal {
  member: Member;
  total: number;
  share: number;
}

@Injectable()
export class SummaryStore {
  private readonly summaryService = inject(SummaryService);
  private readonly transactionService = inject(TransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly householdStore = inject(HouseholdStore);
  private readonly calendar = inject(CalendarStore);

  readonly currentMonth = this.calendar.currentMonth;
  private readonly _year = signal(this.calendar.currentYear());
  readonly year = this._year.asReadonly();

  readonly data = rxResource<MonthSummary[], number>({
    params: () => this._year(),
    stream: ({params}) => this.summaryService.getYearSummary(params),
    defaultValue: [],
  });

  private readonly allMonths = rxResource<MonthSummary[], void>({
    stream: () => this.summaryService.getMonthSummaries(),
    defaultValue: [],
  });

  readonly firstYear = computed(() => {
    const months = this.allMonths.value();
    return months.length === 0 ? this._year() : yearOfMonth(months.at(-1)!.month);
  });
  readonly canGoBack = computed(() => this._year() > this.firstYear());
  readonly canGoForward = computed(() => this._year() < this.calendar.currentYear());

  readonly months = this.data.value;
  private readonly monthsWithData = computed(() => this.months().filter(m => m.income > 0 || m.expenses > 0 || m.savings > 0));
  private readonly averagedMonths = computed(() => {
    const completed = this.monthsWithData().filter(m => m.month < this.currentMonth());
    return completed.length > 0 ? completed : this.monthsWithData();
  });

  readonly income = computed(() => sumBy(this.months(), m => m.income));
  readonly expenses = computed(() => sumBy(this.months(), m => m.expenses));
  readonly savings = computed(() => sumBy(this.months(), m => m.savings));
  readonly balance = computed(() => this.income() - this.expenses() - this.savings());
  readonly savingsRate = computed(() => percentOf(this.savings(), this.income()));
  readonly averageIncome = computed(() => averageOf(this.averagedMonths(), m => m.income));
  readonly averageExpenses = computed(() => averageOf(this.averagedMonths(), m => m.expenses));

  readonly expenseCategories = computed(() => this.itemTotals(TransactionType.EXPENSE, m => m.expensesByCategory, this.expenses()));
  readonly incomeSources = computed(() => this.itemTotals(TransactionType.INCOME, m => m.incomesBySource, this.income()));

  readonly sharedExpenses = computed(() => sumBy(this.months(), m => m.sharedExpenses));
  readonly personalExpenses = computed(() => this.expenses() - this.sharedExpenses());

  readonly memberTotals = computed<MemberYearTotal[]>(() => this.householdStore.everyone()
    .map(member => {
      const total = sumBy(this.months(), m => m.personalExpensesByMember.find(e => e.memberId === member.id)?.amount ?? 0);
      return {member, total, share: percentOf(total, this.personalExpenses())};
    })
    .filter(total => this.householdStore.isCurrentMember(total.member.id) || total.total > 0));

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.data);
    reloadWhenChanged(this.transactionService.revision, this.allMonths);
  }

  previousYear(): void {
    this._year.update(year => year - 1);
  }

  nextYear(): void {
    this._year.update(year => year + 1);
  }

  private itemTotals(type: TransactionType, amountsOf: (month: MonthSummary) => ItemAmount[], grandTotal: number): ItemYearTotal[] {
    return this.configStore.itemsOf(type)
      .map(item => {
        const byMonth = this.months().map(month => amountsOf(month).find(entry => entry.itemId === item.id)?.amount ?? 0);
        const total = byMonth.reduce((sum, amount) => sum + amount, 0);
        return {item, total, share: percentOf(total, grandTotal), byMonth};
      })
      .filter(entry => entry.total > 0)
      .sort((a, b) => b.total - a.total);
  }
}

function sumBy(months: MonthSummary[], valueOf: (month: MonthSummary) => number): number {
  return months.reduce((sum, month) => sum + valueOf(month), 0);
}

function averageOf(months: MonthSummary[], valueOf: (month: MonthSummary) => number): number {
  return months.length === 0 ? 0 : sumBy(months, valueOf) / months.length;
}
