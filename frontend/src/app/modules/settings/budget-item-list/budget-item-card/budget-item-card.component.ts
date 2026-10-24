import {ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal} from '@angular/core';
import {form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {BudgetItem, SavingsGoal} from '../../../../models/BudgetItem';
import {HouseholdStore} from '../../../../stores/household.store';
import {formatMoney} from '../../../../shared/utils/money';
import {TransactionType} from '../../../../models/Transaction';
import {BudgetItemEditorStore} from '../../stories/budget-item-editor-store';
import {BudgetItemSaverService} from '../../services/budget-item-saver.service';
import {editBudgetItemModel} from '../../budget-item-editor/models/form-budget-item';
import {showServerError, showSuccess} from '../../../../utils/NotificationHelper';

@Component({
  selector: 'app-budget-item-card',
  templateUrl: './budget-item-card.component.html',
  styleUrl: './budget-item-card.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetItemCardComponent {
  private readonly editor = inject(BudgetItemEditorStore);
  private readonly saver = inject(BudgetItemSaverService);
  private readonly messageService = inject(MessageService);
  private readonly householdStore = inject(HouseholdStore);

  readonly item = input.required<BudgetItem>();
  readonly kind = input.required<TransactionType>();

  readonly activeForm = form(linkedSignal(() => ({active: this.item().active})));
  readonly meta = computed(() => {
    const status = this.item().active ? 'Aktywna' : 'Wstrzymana';
    switch (this.kind()) {
      case TransactionType.EXPENSE:
      case TransactionType.INCOME:
        return status;
      case TransactionType.SAVING:
        return this.goalMeta(this.item() as SavingsGoal);
    }
  });

  edit(): void {
    this.editor.edit(this.kind(), this.item());
  }

  toggleActive(): void {
    const item = this.item();
    const model = {...editBudgetItemModel(item), active: !item.active};
    this.saver.save(this.kind(), model).subscribe({
      next: () => showSuccess(model.active ? 'Przywrócono' : 'Wstrzymano', item.name, this.messageService),
      error: (error) => showServerError('Nie udało się zmienić statusu', error, this.messageService),
    });
  }

  private goalMeta(goal: SavingsGoal): string {
    const owner = goal.ownerId === null ? 'Wspólny' : this.householdStore.memberOf(goal.ownerId)?.firstName ?? '';
    return `${owner} · ${formatMoney(goal.targetAmount, 'whole')}`;
  }
}
