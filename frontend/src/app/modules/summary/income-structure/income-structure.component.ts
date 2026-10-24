import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {ChartConfiguration} from 'chart.js';
import {SummaryStore} from '../stories/summary-store';
import {AppearanceService} from '../../../services/appearance.service';
import {chartTheme, moneyTooltip} from '../../../shared/utils/chart-theme';

@Component({
  selector: 'app-income-structure',
  templateUrl: './income-structure.component.html',
  styleUrl: './income-structure.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncomeStructureComponent {
  readonly store = inject(SummaryStore);
  private readonly appearance = inject(AppearanceService);

  readonly chart = computed<ChartConfiguration>(() => {
    const theme = chartTheme(this.appearance.darkMode());
    const sources = this.store.incomeSources();
    return {
      type: 'doughnut',
      data: {
        labels: sources.map(source => source.item.name),
        datasets: [{
          data: sources.map(source => source.total),
          backgroundColor: sources.map(source => source.item.color),
          borderColor: theme.surface,
          borderWidth: 3,
          borderRadius: 6,
        }],
      },
      options: {
        cutout: '70%',
        maintainAspectRatio: false,
        plugins: {legend: {display: false}, tooltip: moneyTooltip(theme)},
      },
    };
  });
}
