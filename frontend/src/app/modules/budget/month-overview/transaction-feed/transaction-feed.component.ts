import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {MonthStore} from '../../stories/month-store';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {TransactionEditorStore} from '../../../../stores/transaction-editor.store';
import {Transaction, TransactionType} from '../../../../models/Transaction';
import {BudgetItem} from '../../../../models/BudgetItem';
import {Member} from '../../../../models/Member';
import {ALL_TYPES, FeedFilter} from '../../models/month-view';

interface FeedFilterOption {
  filter: FeedFilter;
  label: string;
}

const AMOUNT_SIGNS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: '−',
  [TransactionType.INCOME]: '+',
  [TransactionType.SAVING]: '',
};

@Component({
  selector: 'app-transaction-feed',
  templateUrl: './transaction-feed.component.html',
  styleUrl: './transaction-feed.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionFeedComponent {
  readonly store = inject(MonthStore);
  readonly editor = inject(TransactionEditorStore);
  private readonly configStore = inject(BudgetConfigStore);
  readonly householdStore = inject(HouseholdStore);

  readonly filterOptions: FeedFilterOption[] = [
    {filter: ALL_TYPES, label: 'Wszystkie'},
    {filter: TransactionType.EXPENSE, label: 'Wydatki'},
    {filter: TransactionType.INCOME, label: 'Wpływy'},
    {filter: TransactionType.SAVING, label: 'Oszczędności'},
  ];

  readonly selectedCategory = computed(() => {
    const categoryId = this.store.categoryFilter();
    return categoryId === null ? undefined : this.configStore.itemOf(TransactionType.EXPENSE, categoryId);
  });

  readonly isFirstLoad = computed(() => this.store.data.isLoading() && this.store.transactions().length === 0);

  itemOf(transaction: Transaction): BudgetItem | undefined {
    return this.configStore.itemOf(transaction.type, transaction.itemId);
  }

  memberOf(transaction: Transaction): Member | undefined {
    return this.householdStore.memberOf(transaction.memberId);
  }

  signOf(transaction: Transaction): string {
    return AMOUNT_SIGNS[transaction.type];
  }

  onSearch(event: Event): void {
    this.store.setSearch((event.target as HTMLInputElement).value);
  }
}
