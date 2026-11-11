import {ChangeDetectionStrategy, Component, effect, inject, input} from '@angular/core';
import {MonthStore} from '../stores/month.store';
import {MonthForecastStore} from '../stores/month-forecast.store';
import {MonthFeedSource} from '../stores/month-feed-source';
import {UpcomingRecurringStore} from '../../../stores/upcoming-recurring.store';
import {TransactionFeedStore} from '../../../shared/components/transaction-feed/transaction-feed.store';
import {TransactionFeedSource} from '../../../shared/components/transaction-feed/transaction-feed-source';

@Component({
  selector: 'app-month-overview',
  templateUrl: './month-overview.component.html',
  styleUrl: './month-overview.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    MonthStore,
    MonthForecastStore,
    UpcomingRecurringStore,
    TransactionFeedStore,
    {provide: TransactionFeedSource, useClass: MonthFeedSource},
  ],
})
export class MonthOverviewComponent {
  readonly store = inject(MonthStore);

  readonly month = input.required<string>();

  constructor() {
    effect(() => this.store.setMonth(this.month()));
  }
}
