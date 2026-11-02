import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {RecurringTransactionsStore} from '../stores/recurring-transactions.store';
import {HouseholdStore} from '../../../stores/household.store';
import {TransactionType} from '../../../models/transaction.model';
import {RecurrenceFrequency} from '../../../models/recurring-transaction.model';

interface FrequencyFilterOption {
  value: RecurrenceFrequency | null;
  label: string;
}

const FREQUENCY_FILTERS: FrequencyFilterOption[] = [
  {value: null, label: 'Każda'},
  {value: RecurrenceFrequency.MONTHLY, label: 'Co miesiąc'},
  {value: RecurrenceFrequency.YEARLY, label: 'Co rok'},
];

@Component({
  selector: 'app-recurring-transactions',
  templateUrl: './recurring-transactions.component.html',
  styleUrl: './recurring-transactions.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [RecurringTransactionsStore],
})
export class RecurringTransactionsComponent {
  readonly store = inject(RecurringTransactionsStore);
  readonly householdStore = inject(HouseholdStore);

  readonly frequencyFilters = FREQUENCY_FILTERS;
  readonly countOfType = (type: TransactionType | null) => this.store.countOfType(type);

  onSearch(event: Event): void {
    this.store.setSearch((event.target as HTMLInputElement).value);
  }
}
