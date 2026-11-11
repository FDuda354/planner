import {inject, Injectable} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {RecurringTransactionService} from '../services/recurring-transaction.service';
import {TransactionService} from '../services/transaction.service';
import {RecurringTransaction} from '../models/recurring-transaction.model';
import {belongsToNobody} from '../models/transaction.model';
import {reloadWhenChanged} from '../shared/utils/resource';
import {HouseholdStore} from './household.store';

@Injectable()
export class UpcomingRecurringStore {
  private readonly recurringService = inject(RecurringTransactionService);
  private readonly transactionService = inject(TransactionService);
  private readonly householdStore = inject(HouseholdStore);

  private readonly recurring = rxResource<RecurringTransaction[], void>({
    stream: () => this.recurringService.getRecurringTransactions(),
    defaultValue: [],
  });

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.recurring);
  }

  bookedLaterIn(month: string): RecurringTransaction[] {
    return this.recurring.value().filter(recurring => this.isBookedLaterIn(recurring, month));
  }

  private isBookedLaterIn(recurring: RecurringTransaction, month: string): boolean {
    return recurring.active
           && recurring.nextOccurrence.startsWith(month)
           && (belongsToNobody(recurring) || this.householdStore.isCurrentMember(recurring.memberId));
  }
}
