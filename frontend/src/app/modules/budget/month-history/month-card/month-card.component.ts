import {ChangeDetectionStrategy, Component, computed, inject, input} from '@angular/core';
import {MonthSummary} from '../../../../models/Summary';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {TransactionType} from '../../../../models/Transaction';
import {BudgetItem} from '../../../../models/BudgetItem';
import {monthName, yearOfMonth} from '../../../../shared/utils/month';
import {percentOf} from '../../../../shared/utils/money';
import {monthDetailRoute} from '../../../../app-routes';

interface CategorySlice {
  item: BudgetItem;
  share: number;
}

const VISIBLE_SLICES = 5;

@Component({
  selector: 'app-month-card',
  templateUrl: './month-card.component.html',
  styleUrl: './month-card.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthCardComponent {
  private readonly configStore = inject(BudgetConfigStore);

  readonly summary = input.required<MonthSummary>();

  readonly route = computed(() => monthDetailRoute(this.summary().month));
  readonly name = computed(() => monthName(this.summary().month));
  readonly year = computed(() => yearOfMonth(this.summary().month));
  readonly balance = computed(() => this.summary().income - this.summary().expenses - this.summary().savings);

  readonly slices = computed<CategorySlice[]>(() => this.summary().expensesByCategory
    .slice(0, VISIBLE_SLICES)
    .flatMap(entry => {
      const item = this.configStore.itemOf(TransactionType.EXPENSE, entry.itemId);
      return item ? [{item, share: percentOf(entry.amount, this.summary().expenses)}] : [];
    }));

  readonly restShare = computed(() => 1 - this.slices().reduce((sum, slice) => sum + slice.share, 0));
  readonly topCategory = computed(() => this.slices()[0]);
}
