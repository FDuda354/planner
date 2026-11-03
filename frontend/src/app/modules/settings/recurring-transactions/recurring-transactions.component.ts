import {ChangeDetectionStrategy, Component, effect, inject, input, untracked} from '@angular/core';
import {Router} from '@angular/router';
import {RecurringTransactionsStore} from '../stores/recurring-transactions.store';
import {HouseholdStore} from '../../../stores/household.store';
import {TransactionType} from '../../../models/transaction.model';
import {RecurrenceFrequency, RecurringTransaction} from '../../../models/recurring-transaction.model';

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
  private readonly router = inject(Router);

  readonly editId = input<string>();
  readonly frequencyFilters = FREQUENCY_FILTERS;
  readonly countOfType = (type: TransactionType | null) => this.store.countOfType(type);

  constructor() {
    effect(() => {
      const requested = this.store.recurringTransactions().find(recurring => String(recurring.id) === this.editId());
      if (requested) {
        untracked(() => this.openRequestedEditor(requested));
      }
    });
  }

  onSearch(event: Event): void {
    this.store.setSearch((event.target as HTMLInputElement).value);
  }

  private openRequestedEditor(recurring: RecurringTransaction): void {
    this.store.edit(recurring);
    void this.router.navigate([], {queryParams: {edit: null}, queryParamsHandling: 'merge', replaceUrl: true});
  }
}
