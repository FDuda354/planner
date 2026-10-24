import {ChangeDetectionStrategy, Component, effect, inject, input} from '@angular/core';
import {MonthStore} from '../stories/month-store';

@Component({
  selector: 'app-month-overview',
  templateUrl: './month-overview.component.html',
  styleUrl: './month-overview.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MonthStore],
})
export class MonthOverviewComponent {
  readonly store = inject(MonthStore);

  readonly month = input.required<string>();

  constructor() {
    effect(() => this.store.setMonth(this.month()));
  }
}
