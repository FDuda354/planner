import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {MonthStore} from '../../stores/month.store';
import {TransactionFeedStore} from '../../stores/transaction-feed.store';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {TransactionEditorStore} from '../../../../stores/transaction-editor.store';
import {TransactionDeletionService} from '../../../../services/transaction-deletion.service';
import {amountSignOf, belongsToNobody, Transaction, TransactionType} from '../../../../models/transaction.model';
import {BudgetItem} from '../../../../models/budget-item.model';
import {Member} from '../../../../models/member.model';

@Component({
  selector: 'app-transaction-feed',
  templateUrl: './transaction-feed.component.html',
  styleUrl: './transaction-feed.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionFeedComponent {
  readonly store = inject(MonthStore);
  readonly feed = inject(TransactionFeedStore);
  readonly editor = inject(TransactionEditorStore);
  private readonly deletion = inject(TransactionDeletionService);
  private readonly configStore = inject(BudgetConfigStore);
  readonly householdStore = inject(HouseholdStore);

  readonly selectedCategory = computed(() => {
    const categoryId = this.feed.filters().categoryId;
    return categoryId === null ? undefined : this.configStore.itemOf(TransactionType.EXPENSE, categoryId);
  });

  readonly isFirstLoad = computed(() => this.store.data.isLoading() && this.store.transactions().length === 0);
  readonly countOfType = (type: TransactionType | null) => this.feed.countOfType(type);

  itemOf(transaction: Transaction): BudgetItem | undefined {
    return this.configStore.itemOf(transaction.type, transaction.itemId);
  }

  isAssignedToMember(transaction: Transaction): boolean {
    return !belongsToNobody(transaction);
  }

  memberOf(transaction: Transaction): Member | undefined {
    return this.householdStore.memberOf(transaction.memberId);
  }

  signOf(transaction: Transaction): string {
    return amountSignOf(transaction.type);
  }

  deleteLabel(transaction: Transaction): string {
    return `Usuń transakcję ${transaction.note || (this.itemOf(transaction)?.name ?? '')}`;
  }

  confirmDeletion(transaction: Transaction, event: Event): void {
    this.deletion.confirmDeletion(transaction, event.currentTarget);
  }

  onSearch(event: Event): void {
    this.feed.setSearch((event.target as HTMLInputElement).value);
  }
}
