import {computed, inject, Injectable} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {RecurringTransactionService} from '../../../services/recurring-transaction.service';
import {TransactionService} from '../../../services/transaction.service';
import {HouseholdStore} from '../../../stores/household.store';
import {RecurringTransaction} from '../../../models/recurring-transaction.model';
import {belongsToNobody, netAmountOf} from '../../../models/transaction.model';
import {reloadWhenChanged} from '../../../shared/utils/resource';
import {MonthStore} from './month.store';

@Injectable()
export class MonthForecastStore {
  private readonly recurringService = inject(RecurringTransactionService);
  private readonly transactionService = inject(TransactionService);
  private readonly householdStore = inject(HouseholdStore);
  private readonly monthStore = inject(MonthStore);

  private readonly recurring = rxResource<RecurringTransaction[], void>({
    stream: () => this.recurringService.getRecurringTransactions(),
    defaultValue: [],
  });

  readonly upcomingRecurring = computed(() => this.recurring.value().filter(recurring => this.isBookedLaterThisMonth(recurring)));
  private readonly upcomingRecurringNet = computed(() => netAmountOf(this.upcomingRecurring()));
  readonly forecastBalance = computed(() => this.monthStore.balance() + this.monthStore.plannedNet() + this.upcomingRecurringNet());

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.recurring);
  }

  private isBookedLaterThisMonth(recurring: RecurringTransaction): boolean {
    return recurring.active
           && recurring.nextOccurrence.startsWith(this.monthStore.month())
           && (belongsToNobody(recurring) || this.householdStore.isCurrentMember(recurring.memberId));
  }
}
