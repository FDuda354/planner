import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {MonthStore} from '../../stories/month-store';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {TransactionType} from '../../../../models/Transaction';
import {monthLabel, monthNameInLocative, shiftMonth} from '../../../../shared/utils/month';
import {percentOf} from '../../../../shared/utils/money';

interface FlowSegment {
  kind: 'expense' | 'saving' | 'free';
  share: number;
}

@Component({
  selector: 'app-month-balance',
  templateUrl: './month-balance.component.html',
  styleUrl: './month-balance.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthBalanceComponent {
  readonly store = inject(MonthStore);
  private readonly configStore = inject(BudgetConfigStore);

  readonly title = computed(() => monthLabel(this.store.month()));
  readonly isOverspent = computed(() => this.store.balance() < 0);

  readonly flow = computed<FlowSegment[]>(() => {
    const base = Math.max(this.store.income(), this.store.expenses() + this.store.savings());
    return [
      {kind: 'expense', share: percentOf(this.store.expenses(), base)},
      {kind: 'saving', share: percentOf(this.store.savings(), base)},
      {kind: 'free', share: percentOf(Math.max(this.store.balance(), 0), base)},
    ];
  });

  readonly spentShare = computed(() => percentOf(this.store.expenses(), this.store.income()));
  readonly savedShare = computed(() => percentOf(this.store.savings(), this.store.income()));
  readonly freeShare = computed(() => percentOf(Math.max(this.store.balance(), 0), this.store.income()));
  readonly previousMonthInLocative = computed(() => monthNameInLocative(shiftMonth(this.store.month(), -1)));

  readonly biggestExpenseItem = computed(() => {
    const biggest = this.store.biggestExpense();
    return biggest ? this.configStore.itemOf(TransactionType.EXPENSE, biggest.itemId) : undefined;
  });

  readonly changeAbs = computed(() => Math.abs(this.store.expensesChange() ?? 0));
}
