import {ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked} from '@angular/core';
import {apply, disabled, form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {finalize} from 'rxjs';
import {RecurringEditorState, RecurringTransactionsStore} from '../../stories/recurring-transactions-store';
import {RecurringTransactionService} from '../../../../services/recurring-transaction.service';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {AuthService} from '../../../auth/services/auth.service';
import {TRANSACTION_TYPE_OPTIONS, TransactionType} from '../../../../models/Transaction';
import {BudgetItem} from '../../../../models/BudgetItem';
import {prepareFormSubmission} from '../../../../shared/forms/prepare-form-submission';
import {showServerError, showSuccess} from '../../../../utils/NotificationHelper';
import {editRecurringModel, newRecurringModel, RecurringFormModel, recurringSchema, toRecurringRequest} from '../models/form-recurring-transaction';

@Component({
  selector: 'app-recurring-transaction-editor',
  templateUrl: './recurring-transaction-editor.component.html',
  styleUrl: './recurring-transaction-editor.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecurringTransactionEditorComponent {
  private readonly store = inject(RecurringTransactionsStore);
  private readonly recurringService = inject(RecurringTransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  readonly householdStore = inject(HouseholdStore);

  readonly typeOptions = TRANSACTION_TYPE_OPTIONS;
  readonly sharedInputId = 'recurring-shared';
  readonly state = this.store.editor;
  readonly isEdit = computed(() => this.state().recurring !== null);
  readonly isWorking = signal(false);

  readonly model = signal<RecurringFormModel>(newRecurringModel(null));
  readonly recurringForm = form(this.model, (path) => {
    apply(path, recurringSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  readonly items = computed(() => this.selectableItems(this.model().type));
  readonly isExpense = computed(() => this.model().type === TransactionType.EXPENSE);

  constructor() {
    effect(() => {
      const state = this.state();
      if (state.visible) {
        untracked(() => this.recurringForm().reset(this.initialModel(state)));
      }
    });
  }

  selectType(type: TransactionType): void {
    this.model.update(model => ({...model, type, itemId: null, shared: true}));
  }

  selectItem(itemId: number): void {
    this.recurringForm.itemId().value.set(itemId);
  }

  selectMember(memberId: number): void {
    this.recurringForm.memberId().value.set(memberId);
  }

  onVisibleChange(visible: boolean): void {
    if (!visible) {
      this.store.close();
    }
  }

  save(event: SubmitEvent): void {
    event.preventDefault();
    if (!prepareFormSubmission(this.recurringForm, this.isWorking())) {
      return;
    }
    const request = toRecurringRequest(this.model());
    this.isWorking.set(true);
    this.recurringService.saveRecurringTransaction(request)
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          showSuccess('Zapisano stałą transakcję', this.configStore.itemOf(request.type, request.itemId)?.name ?? '', this.messageService);
          this.store.reload();
          this.store.close();
        },
        error: (error) => showServerError('Nie udało się zapisać stałej transakcji', error, this.messageService),
      });
  }

  private initialModel(state: RecurringEditorState): RecurringFormModel {
    return state.recurring ? editRecurringModel(state.recurring) : newRecurringModel(this.authService.currentMember()?.id ?? null);
  }

  private selectableItems(type: TransactionType): BudgetItem[] {
    const editing = this.state().recurring;
    const editedItemId = editing?.type === type ? editing.itemId : null;
    return this.configStore.itemsOf(type).filter(item => item.active || item.id === editedItemId);
  }
}
