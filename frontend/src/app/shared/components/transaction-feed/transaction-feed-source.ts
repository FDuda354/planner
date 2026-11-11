import {Signal} from '@angular/core';
import {Transaction} from '../../../models/transaction.model';
import {RecurringTransaction} from '../../../models/recurring-transaction.model';

export abstract class TransactionFeedSource {
  abstract readonly period: Signal<string>;
  abstract readonly transactions: Signal<Transaction[]>;
  abstract readonly upcomingRecurring: Signal<RecurringTransaction[]>;
  abstract readonly isLoading: Signal<boolean>;
}
