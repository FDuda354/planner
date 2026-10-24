import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {SummaryStore} from '../stories/summary-store';
import {BudgetItem} from '../../../models/BudgetItem';
import {formatMoney} from '../../../shared/utils/money';
import {monthName, monthShortName} from '../../../shared/utils/month';
import {AppRoute, monthDetailRoute} from '../../../app-routes';

interface HeatCell {
  month: string;
  route: string | null;
  intensity: number;
  tooltip: string;
}

interface HeatRow {
  item: BudgetItem;
  total: number;
  cells: HeatCell[];
}

@Component({
  selector: 'app-category-heatmap',
  templateUrl: './category-heatmap.component.html',
  styleUrl: './category-heatmap.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryHeatmapComponent {
  readonly store = inject(SummaryStore);

  readonly monthLabels = computed(() => this.store.months().map(m => monthShortName(m.month)));

  readonly rows = computed<HeatRow[]>(() => this.store.expenseCategories().map(category => {
    const currentMonth = this.store.currentMonth();
    const peak = Math.max(...category.byMonth);
    return {
      item: category.item,
      total: category.total,
      cells: category.byMonth.map((amount, index) => {
        const month = this.store.months()[index].month;
        return {
          month,
          route: month > currentMonth ? null : this.routeOf(month, currentMonth),
          intensity: peak === 0 ? 0 : amount / peak,
          tooltip: `${monthName(month)}: ${formatMoney(amount)}`,
        };
      }),
    };
  }));

  private routeOf(month: string, currentMonth: string): string {
    return month === currentMonth ? AppRoute.BUDGET : monthDetailRoute(month);
  }
}
