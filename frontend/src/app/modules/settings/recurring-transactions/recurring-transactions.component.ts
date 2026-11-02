import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {form} from '@angular/forms/signals';
import {RecurringTransactionsStore} from '../stories/recurring-transactions-store';
import {HouseholdStore} from '../../../stores/household.store';
import {TransactionType} from '../../../models/Transaction';
import {RecurrenceFrequency} from '../../../models/RecurringTransaction';

interface FilterOption<T> {
  value: T | null;
  label: string;
}

const TYPE_FILTERS: FilterOption<TransactionType>[] = [
  {value: null, label: 'Wszystkie'},
  {value: TransactionType.EXPENSE, label: 'Wydatki'},
  {value: TransactionType.INCOME, label: 'Wpływy'},
  {value: TransactionType.SAVING, label: 'Oszczędności'},
];

const FREQUENCY_FILTERS: FilterOption<RecurrenceFrequency>[] = [
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

  readonly typeFilters = TYPE_FILTERS;
  readonly frequencyFilters = FREQUENCY_FILTERS;
  readonly filterForm = form(this.store.filters);

  selectType(type: TransactionType | null): void {
    this.filterForm.type().value.set(type);
  }

  selectFrequency(frequency: RecurrenceFrequency | null): void {
    this.filterForm.frequency().value.set(frequency);
  }
}
