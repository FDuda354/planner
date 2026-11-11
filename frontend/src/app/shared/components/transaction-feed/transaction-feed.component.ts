import {ChangeDetectionStrategy, Component, ElementRef, inject, input, signal} from '@angular/core';
import {PaginatorState} from 'primeng/paginator';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {HouseholdStore} from '../../../stores/household.store';
import {TransactionEditorStore} from '../../../stores/transaction-editor.store';
import {TransactionDeletionService} from '../../../services/transaction-deletion.service';
import {amountSignOf, belongsToNobody, Transaction} from '../../../models/transaction.model';
import {BudgetItem} from '../../../models/budget-item.model';
import {Member} from '../../../models/member.model';
import {AppRoute} from '../../../app-routes';
import {FEED_PAGE_SIZE, TransactionFeedStore} from './transaction-feed.store';

type FeedRowEntry = Pick<Transaction, 'type' | 'itemId' | 'memberId' | 'shared'>;

@Component({
  selector: 'app-transaction-feed',
  templateUrl: './transaction-feed.component.html',
  styleUrl: './transaction-feed.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionFeedComponent {
  readonly feed = inject(TransactionFeedStore);
  readonly editor = inject(TransactionEditorStore);
  private readonly deletion = inject(TransactionDeletionService);
  private readonly configStore = inject(BudgetConfigStore);
  readonly householdStore = inject(HouseholdStore);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  protected readonly AppRoute = AppRoute;

  readonly periodLabel = input.required<string>();
  readonly emptyText = input.required<string>();

  readonly pageSize = FEED_PAGE_SIZE;
  readonly plannedExpanded = signal(false);

  togglePlanned(): void {
    this.plannedExpanded.update(expanded => !expanded);
  }

  showPage(state: PaginatorState): void {
    this.feed.showPage(state.first ?? 0);
    this.host.nativeElement.scrollIntoView({behavior: 'smooth', block: 'start'});
  }

  itemOf(entry: FeedRowEntry): BudgetItem | undefined {
    return this.configStore.itemOf(entry.type, entry.itemId);
  }

  isAssignedToMember(entry: FeedRowEntry): boolean {
    return !belongsToNobody(entry);
  }

  memberOf(entry: FeedRowEntry): Member | undefined {
    return this.householdStore.memberOf(entry.memberId);
  }

  signOf(entry: FeedRowEntry): string {
    return amountSignOf(entry.type);
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
