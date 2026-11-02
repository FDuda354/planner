import {ChangeDetectionStrategy, Component, inject, input} from '@angular/core';
import {BudgetItem} from '../../../models/budget-item.model';
import {TransactionType} from '../../../models/transaction.model';
import {BudgetItemEditorStore} from '../stores/budget-item-editor.store';

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

  create(): void {
    this.editor.create(this.kind());
  }
}
