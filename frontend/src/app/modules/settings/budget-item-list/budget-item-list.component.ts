import {ChangeDetectionStrategy, Component, computed, inject, input} from '@angular/core';
import {BudgetItem} from '../../../models/BudgetItem';
import {TransactionType} from '../../../models/Transaction';
import {BudgetItemEditorStore} from '../stories/budget-item-editor-store';

@Component({
  selector: 'app-budget-item-list',
  templateUrl: './budget-item-list.component.html',
  styleUrl: './budget-item-list.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetItemListComponent {
  private readonly editor = inject(BudgetItemEditorStore);

  readonly kind = input.required<TransactionType>();
  readonly items = input.required<BudgetItem[]>();
  readonly addLabel = input.required<string>();

  readonly sortedItems = computed(() => [...this.items()].sort((a, b) => Number(b.active) - Number(a.active)));

  create(): void {
    this.editor.create(this.kind());
  }
}
