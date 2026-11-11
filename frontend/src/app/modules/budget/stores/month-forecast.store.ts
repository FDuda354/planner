import {computed, inject, Injectable} from '@angular/core';
import {UpcomingRecurringStore} from '../../../stores/upcoming-recurring.store';
import {netAmountOf} from '../../../models/transaction.model';
import {MonthStore} from './month.store';

@Injectable()
export class MonthForecastStore {
  private readonly upcomingRecurringStore = inject(UpcomingRecurringStore);
  private readonly monthStore = inject(MonthStore);

  readonly upcomingRecurring = computed(() => this.upcomingRecurringStore.bookedLaterIn(this.monthStore.month()));
  private readonly upcomingRecurringNet = computed(() => netAmountOf(this.upcomingRecurring()));
  readonly forecastBalance = computed(() => this.monthStore.balance() + this.monthStore.plannedNet() + this.upcomingRecurringNet());
}
