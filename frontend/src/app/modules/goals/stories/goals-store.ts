import {computed, inject, Injectable, signal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {SummaryService} from '../../../services/summary.service';
import {TransactionService} from '../../../services/transaction.service';
import {GoalProgress} from '../../../models/Summary';
import {SavingsGoal} from '../../../models/BudgetItem';
import {monthLabelInGenitive, monthShortName, shiftMonth} from '../../../shared/utils/month';
import {CalendarStore} from '../../../stores/calendar.store';
import {percentOf} from '../../../shared/utils/money';
import {reloadWhenChanged} from '../../../shared/utils/resource';

const AVERAGE_WINDOW_MONTHS = 3;
const HISTORY_MONTHS = 6;

export type OwnerFilter = 'ALL' | 'SHARED' | number;

export interface MonthContribution {
  month: string;
  label: string;
  amount: number;
}

export interface GoalView {
  goal: SavingsGoal;
  saved: number;
  progress: number;
  remaining: number;
  monthlyAverage: number;
  forecast: string | null;
  history: MonthContribution[];
}

@Injectable()
export class GoalsStore {
  private readonly summaryService = inject(SummaryService);
  private readonly transactionService = inject(TransactionService);
  private readonly calendar = inject(CalendarStore);

  readonly data = rxResource<GoalProgress[], void>({
    stream: () => this.summaryService.getGoalProgress(),
    defaultValue: [],
  });

  private readonly _ownerFilter = signal<OwnerFilter>('ALL');
  readonly ownerFilter = this._ownerFilter.asReadonly();

  readonly goals = computed(() => this.data.value()
    .map(progress => toGoalView(progress, this.calendar.currentMonth()))
    .filter(view => this.matchesOwner(view.goal)));
  readonly activeGoals = computed(() => this.goals().filter(view => view.goal.active));
  readonly pausedGoals = computed(() => this.goals().filter(view => !view.goal.active));
  readonly totalSaved = computed(() => this.activeGoals().reduce((sum, view) => sum + view.saved, 0));
  readonly totalTarget = computed(() => this.activeGoals().reduce((sum, view) => sum + view.goal.targetAmount, 0));

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.data);
  }

  setOwnerFilter(filter: OwnerFilter): void {
    this._ownerFilter.set(filter);
  }

  private matchesOwner(goal: SavingsGoal): boolean {
    const filter = this._ownerFilter();
    return filter === 'ALL' || (filter === 'SHARED' ? goal.ownerId === null : goal.ownerId === filter);
  }
}

function toGoalView(progress: GoalProgress, currentMonth: string): GoalView {
  const remaining = Math.max(progress.goal.targetAmount - progress.saved, 0);
  const monthlyAverage = averageOfLastMonths(progress, currentMonth, AVERAGE_WINDOW_MONTHS);
  return {
    goal: progress.goal,
    saved: progress.saved,
    progress: percentOf(progress.saved, progress.goal.targetAmount),
    remaining,
    monthlyAverage,
    forecast: forecastOf(remaining, monthlyAverage, currentMonth),
    history: lastMonths(progress, currentMonth, HISTORY_MONTHS),
  };
}

function lastMonths(progress: GoalProgress, currentMonth: string, count: number): MonthContribution[] {
  return Array.from({length: count}, (_, index) => shiftMonth(currentMonth, index - count + 1)).map(month => ({
    month,
    label: monthShortName(month),
    amount: progress.contributions.find(c => c.month === month)?.amount ?? 0,
  }));
}

function averageOfLastMonths(progress: GoalProgress, currentMonth: string, count: number): number {
  const completedMonths = lastMonths(progress, currentMonth, count + 1).slice(0, count);
  return completedMonths.reduce((sum, c) => sum + c.amount, 0) / count;
}

function forecastOf(remaining: number, monthlyAverage: number, currentMonth: string): string | null {
  if (remaining === 0 || monthlyAverage === 0) {
    return null;
  }
  return monthLabelInGenitive(shiftMonth(currentMonth, Math.ceil(remaining / monthlyAverage)));
}
