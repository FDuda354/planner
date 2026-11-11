import {inject, Injectable} from '@angular/core';
import {TransactionFeedSource} from '../../../shared/components/transaction-feed/transaction-feed-source';
import {MonthStore} from './month.store';
import {MonthForecastStore} from './month-forecast.store';

@Injectable()
export class MonthFeedSource extends TransactionFeedSource {
  private readonly monthStore = inject(MonthStore);
  private readonly forecast = inject(MonthForecastStore);

  readonly period = this.monthStore.month;
  readonly transactions = this.monthStore.transactions;
  readonly upcomingRecurring = this.forecast.upcomingRecurring;
  readonly isLoading = this.monthStore.data.isLoading;
}
