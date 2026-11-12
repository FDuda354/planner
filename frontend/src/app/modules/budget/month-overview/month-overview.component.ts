import {ChangeDetectionStrategy, Component, effect, inject, input, untracked} from '@angular/core';
import {Router} from '@angular/router';
import {MonthStore} from '../stores/month.store';
import {MonthForecastStore} from '../stores/month-forecast.store';
import {MonthFeedSource} from '../stores/month-feed-source';
import {UpcomingRecurringStore} from '../../../stores/upcoming-recurring.store';
import {TransactionFeedStore} from '../../../shared/components/transaction-feed/transaction-feed.store';
import {TransactionFeedSource} from '../../../shared/components/transaction-feed/transaction-feed-source';
import {TransactionType} from '../../../models/transaction.model';

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
  private readonly feed = inject(TransactionFeedStore);
  private readonly router = inject(Router);

  readonly month = input.required<string>();
  readonly category = input<string>();

  constructor() {
    effect(() => this.store.setMonth(this.month()));
    effect(() => {
      const category = this.category();
      if (category) {
        untracked(() => this.showCategory(Number(category)));
      }
    });
  }

  private showCategory(categoryId: number): void {
    this.feed.selectItem({type: TransactionType.EXPENSE, itemId: categoryId});
    void this.router.navigate([], {queryParams: {category: null}, queryParamsHandling: 'merge', replaceUrl: true});
  }
}
