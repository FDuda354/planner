import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {ChartConfiguration} from 'chart.js';
import {MonthStore} from '../../stories/month-store';
import {AppearanceService} from '../../../../services/appearance.service';
import {chartTheme, moneyTooltip} from '../../../../shared/utils/chart-theme';
import {formatMoney} from '../../../../shared/utils/money';
import {monthName} from '../../../../shared/utils/month';

@Component({
  selector: 'app-spending-pace',
  templateUrl: './spending-pace.component.html',
  styleUrl: './spending-pace.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpendingPaceComponent {
  readonly store = inject(MonthStore);
  private readonly appearance = inject(AppearanceService);

  readonly currentLabel = computed(() => this.store.isCurrentMonth() ? 'Ten miesiąc' : monthName(this.store.month()));

  readonly chart = computed<ChartConfiguration>(() => {
    const theme = chartTheme(this.appearance.darkMode());
    const days = Math.max(this.store.daysInMonth(), this.store.previousCumulativeExpenses().length);
    const previousMonth = {
      label: this.store.previousMonthName(),
      data: this.store.previousCumulativeExpenses(),
      borderColor: theme.muted,
      borderDash: [6, 6],
      borderWidth: 2,
      pointRadius: 0,
      tension: 0.3,
    };
    return {
      type: 'line',
      data: {
        labels: Array.from({length: days}, (_, index) => String(index + 1)),
        datasets: [
          ...(this.store.hasPreviousMonthExpenses() ? [previousMonth] : []),
          {
            label: this.currentLabel(),
            data: this.store.cumulativeExpenses(),
            borderColor: theme.expense,
            backgroundColor: theme.expense + '22',
            fill: 'origin',
            borderWidth: 3,
            pointRadius: 0,
            pointHoverRadius: 5,
            tension: 0.3,
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        interaction: {mode: 'index', intersect: false},
        animation: {duration: 600},
        scales: {
          x: {
            grid: {display: false},
            border: {display: false},
            ticks: {color: theme.text, maxTicksLimit: 8, maxRotation: 0},
          },
          y: {
            grid: {color: theme.grid},
            border: {display: false},
            ticks: {color: theme.text, maxTicksLimit: 5, callback: (value) => formatMoney(Number(value), 'whole')},
          },
        },
        plugins: {
          legend: {display: false},
          tooltip: {
            ...moneyTooltip(theme),
            callbacks: {
              title: (items) => `Dzień ${items[0].label}`,
              label: (context) => ` ${context.dataset.label}: ${formatMoney(Number(context.raw))}`,
            },
          },
        },
      },
    };
  });
}
