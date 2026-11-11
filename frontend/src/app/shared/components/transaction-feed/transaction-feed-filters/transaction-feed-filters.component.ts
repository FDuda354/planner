import {ChangeDetectionStrategy, Component, computed, inject, linkedSignal} from '@angular/core';
import {form} from '@angular/forms/signals';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {itemGroupLabelOf, TransactionType} from '../../../../models/transaction.model';
import {TransactionFeedStore} from '../transaction-feed.store';
import {ItemFilterGroup} from '../transaction-feed.model';

@Component({
  selector: 'app-transaction-feed-filters',
  templateUrl: './transaction-feed-filters.component.html',
  styleUrl: './transaction-feed-filters.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionFeedFiltersComponent {
  readonly feed = inject(TransactionFeedStore);
  readonly householdStore = inject(HouseholdStore);
  private readonly configStore = inject(BudgetConfigStore);

  readonly countOfType = (type: TransactionType | null) => this.feed.countOfType(type);
  readonly itemForm = form(linkedSignal(() => ({item: this.feed.filters().item})));
  readonly itemGroups = computed<ItemFilterGroup[]>(() => this.visibleTypes()
    .map(type => ({
      label: itemGroupLabelOf(type),
      items: this.configStore.itemsOf(type).map(item => ({label: item.name, value: {type, itemId: item.id}, item})),
    }))
    .filter(group => group.items.length > 0));

  private visibleTypes(): TransactionType[] {
    const selectedType = this.feed.filters().type;
    return selectedType === null ? Object.values(TransactionType) : [selectedType];
  }
}
