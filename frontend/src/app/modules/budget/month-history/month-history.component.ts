import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {Router} from '@angular/router';
import {ChartConfiguration} from 'chart.js';
import {HistoryStore} from '../stories/history-store';
import {AppearanceService} from '../../../services/appearance.service';
import {chartTheme, moneyTooltip} from '../../../shared/utils/chart-theme';
import {formatMoney} from '../../../shared/utils/money';
import {monthShortName} from '../../../shared/utils/month';
import {monthDetailRoute} from '../../../app-routes';
import {MonthSummary} from '../../../models/Summary';

@Component({
  selector: 'app-month-history',
  templateUrl: './month-history.component.html',
  styleUrl: './month-history.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [HistoryStore],
})
export class MonthHistoryComponent {
  readonly store = inject(HistoryStore);
  private readonly router = inject(Router);
  private readonly appearance = inject(AppearanceService);

  readonly trendChart = computed<ChartConfiguration>(() => {
    const theme = chartTheme(this.appearance.darkMode());
    const months = this.store.calendarMonths();
    const summaries = this.store.summaryByMonth();
    const valueOf = (month: string, pick: (summary: MonthSummary) => number) => {
      const summary = summaries.get(month);
      return summary ? pick(summary) : null;
    };
    return {
      type: 'bar',
      data: {
        labels: months.map(month => monthShortName(month)),
        datasets: [
          {label: 'Wpływy', data: months.map(month => valueOf(month, s => s.income)), backgroundColor: theme.income, borderRadius: 6, maxBarThickness: 18},
          {label: 'Wydatki', data: months.map(month => valueOf(month, s => s.expenses)), backgroundColor: theme.expense, borderRadius: 6, maxBarThickness: 18},
          {label: 'Odłożone', data: months.map(month => valueOf(month, s => s.savings)), backgroundColor: theme.saving, borderRadius: 6, maxBarThickness: 18},
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
            ticks: {color: theme.text, maxTicksLimit: 5, callback: (value) => formatMoney(Number(value), 'whole')},
          },
        },
        plugins: {legend: {display: false}, tooltip: moneyTooltip(theme)},
        onClick: (_event, elements) => {
          if (elements.length > 0) {
            this.openMonth(months[elements[0].index]);
          }
        },
        onHover: (event, elements) => {
          (event.native?.target as HTMLElement | undefined)?.style.setProperty('cursor', elements.length > 0 ? 'pointer' : 'default');
        },
      },
    };
  });

  openMonth(month: string): void {
    void this.router.navigateByUrl(monthDetailRoute(month));
  }
}
