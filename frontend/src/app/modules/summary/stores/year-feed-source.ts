import {computed, inject, Injectable} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {TransactionService} from '../../../services/transaction.service';
import {CalendarStore} from '../../../stores/calendar.store';
import {UpcomingRecurringStore} from '../../../stores/upcoming-recurring.store';
import {Transaction} from '../../../models/transaction.model';
import {reloadWhenChanged} from '../../../shared/utils/resource';
import {TransactionFeedSource} from '../../../shared/components/transaction-feed/transaction-feed-source';
import {SummaryStore} from './summary.store';

@Injectable()
export class YearFeedSource extends TransactionFeedSource {
  private readonly summaryStore = inject(SummaryStore);
  private readonly transactionService = inject(TransactionService);
  private readonly upcomingRecurringStore = inject(UpcomingRecurringStore);
  private readonly calendar = inject(CalendarStore);

  private readonly data = rxResource<Transaction[], number>({
    params: () => this.summaryStore.year(),
    stream: ({params}) => this.transactionService.getYearTransactions(params),
    defaultValue: [],
  });

  readonly period = computed(() => String(this.summaryStore.year()));
  readonly transactions = this.data.value;
  readonly isLoading = this.data.isLoading;
  readonly upcomingRecurring = computed(() => this.summaryStore.isCurrentYear()
    ? this.upcomingRecurringStore.bookedLaterIn(this.calendar.currentMonth())
    : []);

  constructor() {
    super();
    reloadWhenChanged(this.transactionService.revision, this.data);
  }
}
