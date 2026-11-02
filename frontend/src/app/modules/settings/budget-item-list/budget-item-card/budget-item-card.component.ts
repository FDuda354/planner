import {ChangeDetectionStrategy, Component, computed, inject, input} from '@angular/core';
import {BudgetItem, SavingsGoal} from '../../../../models/budget-item.model';
import {HouseholdStore} from '../../../../stores/household.store';
import {formatMoney} from '../../../../shared/utils/money';
import {TransactionType} from '../../../../models/transaction.model';
import {BudgetItemEditorStore} from '../../stores/budget-item-editor.store';
import {BudgetItemRemoverService} from '../../services/budget-item-remover.service';

@Component({
  selector: 'app-budget-item-card',
  templateUrl: './budget-item-card.component.html',
  styleUrl: './budget-item-card.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetItemCardComponent {
  private readonly editor = inject(BudgetItemEditorStore);
  private readonly remover = inject(BudgetItemRemoverService);
  private readonly householdStore = inject(HouseholdStore);

  readonly item = input.required<BudgetItem>();
  readonly kind = input.required<TransactionType>();

  readonly meta = computed(() => this.kind() === TransactionType.SAVING ? this.goalMeta(this.item() as SavingsGoal) : null);

  edit(): void {
    this.editor.edit(this.kind(), this.item());
  }

  delete(): void {
    this.remover.confirmDeletion(this.kind(), this.item());
  }

  private goalMeta(goal: SavingsGoal): string {
    const owner = goal.ownerId === null ? 'Wspólny' : this.householdStore.memberOf(goal.ownerId)?.firstName ?? '';
    return `${owner} · ${formatMoney(goal.targetAmount, 'whole')}`;
  }
}
