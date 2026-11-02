import {ChangeDetectionStrategy, Component, computed, inject, input} from '@angular/core';
import {GoalView} from '../stores/goals.store';
import {TransactionEditorStore} from '../../../stores/transaction-editor.store';
import {TransactionType} from '../../../models/transaction.model';
import {HouseholdStore} from '../../../stores/household.store';

@Component({
  selector: 'app-goal-card',
  templateUrl: './goal-card.component.html',
  styleUrl: './goal-card.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GoalCardComponent {
  private readonly editor = inject(TransactionEditorStore);
  private readonly householdStore = inject(HouseholdStore);

  readonly view = input.required<GoalView>();

  readonly owner = computed(() => {
    const ownerId = this.view().goal.ownerId;
    return ownerId === null ? undefined : this.householdStore.memberOf(ownerId);
  });
  readonly isReached = computed(() => this.view().remaining === 0);
  readonly historyPeak = computed(() => Math.max(...this.view().history.map(c => c.amount), 1));

  deposit(): void {
    this.editor.create({type: TransactionType.SAVING, itemId: this.view().goal.id});
  }
}
