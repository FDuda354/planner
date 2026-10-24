import {ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked} from '@angular/core';
import {apply, applyWhen, disabled, form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {finalize} from 'rxjs';
import {BudgetItemEditorState, BudgetItemEditorStore} from '../stories/budget-item-editor-store';
import {BudgetItemSaverService} from '../services/budget-item-saver.service';
import {BUDGET_COLORS, BUDGET_ICONS} from '../../../models/BudgetItem';
import {TransactionType} from '../../../models/Transaction';
import {HouseholdStore} from '../../../stores/household.store';
import {prepareFormSubmission} from '../../../shared/forms/prepare-form-submission';
import {showServerError, showSuccess} from '../../../utils/NotificationHelper';
import {budgetItemSchema, BudgetItemFormModel, editBudgetItemModel, newBudgetItemModel, savingsGoalSchema} from './models/form-budget-item';

const KIND_NAMES: Record<TransactionType, { create: string; edit: string; placeholder: string }> = {
  [TransactionType.EXPENSE]: {create: 'Nowa kategoria wydatków', edit: 'Edytuj kategorię', placeholder: 'np. Jedzenie'},
  [TransactionType.INCOME]: {create: 'Nowe źródło dochodu', edit: 'Edytuj źródło dochodu', placeholder: 'np. Pensja'},
  [TransactionType.SAVING]: {create: 'Nowy cel oszczędnościowy', edit: 'Edytuj cel', placeholder: 'np. Wakacje'},
};

@Component({
  selector: 'app-budget-item-editor',
  templateUrl: './budget-item-editor.component.html',
  styleUrl: './budget-item-editor.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetItemEditorComponent {
  private readonly editor = inject(BudgetItemEditorStore);
  private readonly saver = inject(BudgetItemSaverService);
  private readonly messageService = inject(MessageService);
  readonly householdStore = inject(HouseholdStore);

  readonly icons = BUDGET_ICONS;
  readonly colors = BUDGET_COLORS;

  readonly state = this.editor.state;
  readonly isGoal = computed(() => this.state().kind === TransactionType.SAVING);
  readonly isWorking = signal(false);
  readonly header = computed(() => this.state().item ? KIND_NAMES[this.state().kind].edit : KIND_NAMES[this.state().kind].create);
  readonly placeholder = computed(() => KIND_NAMES[this.state().kind].placeholder);

  readonly model = signal<BudgetItemFormModel>(newBudgetItemModel());
  readonly itemForm = form(this.model, (path) => {
    apply(path, budgetItemSchema);
    applyWhen(path, () => this.isGoal(), savingsGoalSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  constructor() {
    effect(() => {
      const state = this.state();
      if (state.visible) {
        untracked(() => this.itemForm().reset(this.initialModel(state)));
      }
    });
  }

  selectIcon(icon: string): void {
    this.itemForm.icon().value.set(icon);
  }

  selectColor(color: string): void {
    this.itemForm.color().value.set(color);
  }

  selectOwner(ownerId: number | null): void {
    this.itemForm.ownerId().value.set(ownerId);
  }

  onVisibleChange(visible: boolean): void {
    if (!visible) {
      this.editor.close();
    }
  }

  save(event: SubmitEvent): void {
    event.preventDefault();
    if (!prepareFormSubmission(this.itemForm, this.isWorking())) {
      return;
    }
    this.isWorking.set(true);
    this.saver.save(this.state().kind, this.model())
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          showSuccess('Zapisano', this.model().name, this.messageService);
          this.editor.close();
        },
        error: (error) => showServerError('Nie udało się zapisać', error, this.messageService),
      });
  }

  private initialModel(state: BudgetItemEditorState): BudgetItemFormModel {
    return state.item ? editBudgetItemModel(state.item) : newBudgetItemModel();
  }
}
