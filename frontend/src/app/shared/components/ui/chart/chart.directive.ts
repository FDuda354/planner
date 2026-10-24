import {afterRenderEffect, Directive, ElementRef, inject, input, OnDestroy} from '@angular/core';
import {
  ArcElement, BarController, BarElement, CategoryScale, Chart, ChartConfiguration, ChartType, DoughnutController, Filler, Legend,
  LinearScale, LineController, LineElement, PointElement, Tooltip
} from 'chart.js';

Chart.register(
  ArcElement, BarController, BarElement, CategoryScale, DoughnutController, Filler, Legend,
  LinearScale, LineController, LineElement, PointElement, Tooltip
);

@Directive({
  selector: 'canvas[uiChart]',
  standalone: false,
})
export class ChartDirective implements OnDestroy {
  private readonly element = inject<ElementRef<HTMLCanvasElement>>(ElementRef);
  readonly config = input.required<ChartConfiguration>({alias: 'uiChart'});
  private chart?: Chart;
  private chartType?: ChartType;

  constructor() {
    afterRenderEffect(() => {
      const config = this.config();
      if (this.chart && this.chartType === config.type) {
        this.chart.data = config.data;
        this.chart.options = config.options ?? {};
        this.chart.update();
      } else {
        this.chart?.destroy();
        this.chart = new Chart(this.element.nativeElement, config);
        this.chartType = config.type;
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
