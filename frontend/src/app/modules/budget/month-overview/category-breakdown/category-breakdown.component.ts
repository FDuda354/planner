import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {ChartConfiguration} from 'chart.js';
import {MonthStore} from '../../stories/month-store';
import {AppearanceService} from '../../../../services/appearance.service';
import {chartTheme, moneyTooltip} from '../../../../shared/utils/chart-theme';

const COLLAPSED_LIMIT = 6;

@Component({
  selector: 'app-category-breakdown',
  templateUrl: './category-breakdown.component.html',
  styleUrl: './category-breakdown.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryBreakdownComponent {
  readonly store = inject(MonthStore);
  private readonly appearance = inject(AppearanceService);

  readonly expanded = signal(false);
  readonly hiddenCount = computed(() => Math.max(this.store.categoryShares().length - COLLAPSED_LIMIT, 0));
  readonly visibleShares = computed(() => this.expanded()
    ? this.store.categoryShares()
    : this.store.categoryShares().slice(0, COLLAPSED_LIMIT));

  readonly chart = computed<ChartConfiguration>(() => {
    const theme = chartTheme(this.appearance.darkMode());
    const shares = this.store.categoryShares();
    const selected = this.store.categoryFilter();
    return {
      type: 'doughnut',
      data: {
        labels: shares.map(share => share.item.name),
        datasets: [{
          data: shares.map(share => share.amount),
          backgroundColor: shares.map(share => selected === null || selected === share.item.id ? share.item.color : theme.muted),
          borderColor: theme.surface,
          borderWidth: 3,
          borderRadius: 6,
          hoverOffset: 6,
        }],
      },
      options: {
        cutout: '72%',
        maintainAspectRatio: false,
        animation: {duration: 600},
        plugins: {
          legend: {display: false},
          tooltip: moneyTooltip(theme),
        },
        onClick: (_event, elements) => {
          if (elements.length > 0) {
            this.store.toggleCategoryFilter(shares[elements[0].index].item.id);
          }
        },
      },
    };
  });
}
