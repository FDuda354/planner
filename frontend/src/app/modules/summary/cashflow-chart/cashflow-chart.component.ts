import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {ChartConfiguration} from 'chart.js';
import {SummaryStore} from '../stories/summary-store';
import {AppearanceService} from '../../../services/appearance.service';
import {chartTheme, moneyTooltip} from '../../../shared/utils/chart-theme';
import {formatMoney} from '../../../shared/utils/money';
import {monthShortName} from '../../../shared/utils/month';

@Component({
  selector: 'app-cashflow-chart',
  templateUrl: './cashflow-chart.component.html',
  styleUrl: './cashflow-chart.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CashflowChartComponent {
  readonly store = inject(SummaryStore);
  private readonly appearance = inject(AppearanceService);

  readonly chart = computed<ChartConfiguration>(() => {
    const theme = chartTheme(this.appearance.darkMode());
    const months = this.store.months();
    const currentMonth = this.store.currentMonth();
    const valueOrGap = (month: string, value: number) => month > currentMonth ? null : value;
    return {
      type: 'bar',
      data: {
        labels: months.map(m => monthShortName(m.month)),
        datasets: [
          {
            type: 'line',
            label: 'Zostało',
            data: months.map(m => valueOrGap(m.month, m.income - m.expenses - m.savings)),
            borderColor: theme.text,
            backgroundColor: theme.surface,
            borderWidth: 2,
            pointRadius: 4,
            pointBorderWidth: 2,
            tension: 0.35,
            order: 0,
          },
          {label: 'Wpływy', data: months.map(m => m.income), backgroundColor: theme.income, borderRadius: 6, maxBarThickness: 16, order: 1},
          {label: 'Wydatki', data: months.map(m => m.expenses), backgroundColor: theme.expense, borderRadius: 6, maxBarThickness: 16, order: 1},
          {label: 'Odłożone', data: months.map(m => m.savings), backgroundColor: theme.saving, borderRadius: 6, maxBarThickness: 16, order: 1},
        ],
      },
      options: {
        maintainAspectRatio: false,
        animation: {duration: 600},
        interaction: {mode: 'index', intersect: false},
        scales: {
          x: {grid: {display: false}, border: {display: false}, ticks: {color: theme.text}},
          y: {
            grid: {color: theme.grid},
            border: {display: false},
            ticks: {color: theme.text, maxTicksLimit: 6, callback: (value) => formatMoney(Number(value), 'whole')},
          },
        },
        plugins: {
          legend: {
            position: 'top',
            align: 'end',
            labels: {color: theme.text, usePointStyle: true, pointStyle: 'rectRounded', boxWidth: 10, boxHeight: 10, padding: 16},
          },
          tooltip: moneyTooltip(theme),
        },
      },
    };
  });
}
