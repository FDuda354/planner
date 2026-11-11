import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {SummaryStore} from './stores/summary.store';
import {YearFeedSource} from './stores/year-feed-source';
import {UpcomingRecurringStore} from '../../stores/upcoming-recurring.store';
import {TransactionFeedStore} from '../../shared/components/transaction-feed/transaction-feed.store';
import {TransactionFeedSource} from '../../shared/components/transaction-feed/transaction-feed-source';

@Component({
  selector: 'app-summary',
  templateUrl: './summary.component.html',
  styleUrl: './summary.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    SummaryStore,
    UpcomingRecurringStore,
    TransactionFeedStore,
    {provide: TransactionFeedSource, useClass: YearFeedSource},
  ],
})
export class SummaryComponent {
  readonly store = inject(SummaryStore);
}
