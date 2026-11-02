import {ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked, viewChild} from '@angular/core';
import {apply, disabled, form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {InputNumber} from 'primeng/inputnumber';
import {finalize} from 'rxjs';
import {addDays, endOfMonth, isSameDay} from 'date-fns';
import {TransactionService} from '../../../services/transaction.service';
import {TransactionDeletionService} from '../../../services/transaction-deletion.service';
import {TransactionEditorState, TransactionEditorStore} from '../../../stores/transaction-editor.store';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {HouseholdStore} from '../../../stores/household.store';
import {AuthService} from '../../../modules/auth/services/auth.service';
import {belongsToNobody, isSharedByDefault, itemLabelOf, TRANSACTION_TYPE_OPTIONS, TransactionRequest, TransactionType} from '../../../models/transaction.model';
import {Member} from '../../../models/member.model';
import {monthLabel, parseMonthKey, toMonthKey} from '../../utils/month';
import {prepareFormSubmission} from '../../forms/prepare-form-submission';
import {showServerError, showSuccess} from '../../utils/notifications';
import {transactionLabel} from '../../utils/transaction-label';
import {AppRoute} from '../../../app-routes';
import {editTransactionModel, newTransactionModel, toTransactionRequest, TransactionFormModel, transactionSchema} from './models/form-transaction';

interface QuickDate {
  label: string;
  daysAgo: number;
}

@Component({
  selector: 'app-transaction-dialog',
  templateUrl: './transaction-dialog.component.html',
  styleUrl: './transaction-dialog.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransactionDialogComponent {
  private readonly editor = inject(TransactionEditorStore);
  private readonly transactionService = inject(TransactionService);
  private readonly messageService = inject(MessageService);
  private readonly deletion = inject(TransactionDeletionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly calendar = inject(CalendarStore);
  private readonly householdStore = inject(HouseholdStore);
  private readonly authService = inject(AuthService);
  private readonly amountInput = viewChild(InputNumber);

  readonly typeOptions = TRANSACTION_TYPE_OPTIONS;
  readonly quickDates: QuickDate[] = [
    {label: 'Dziś', daysAgo: 0},
    {label: 'Wczoraj', daysAgo: 1},
    {label: 'Przedwczoraj', daysAgo: 2},
  ];

  readonly lastDayOfMonth = this.calendar.lastDayOfMonth;
  readonly state = this.editor.state;
  readonly isEdit = computed(() => this.state().editing !== null);
  readonly isWorking = signal(false);

  readonly model = signal<TransactionFormModel>(newTransactionModel(this.state().draft, this.calendar.today(), this.currentMemberId()));
  readonly transactionForm = form(this.model, (path) => {
    apply(path, transactionSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  readonly items = computed(() => this.configStore.itemsOf(this.model().type));
  readonly members = computed(() => this.selectableMembers());
  readonly isAssignedToMember = computed(() => !belongsToNobody(this.model()));
  readonly pastMonthLabel = computed(() => this.pastMonthLabelOf(this.model().date));
  readonly itemLabel = computed(() => itemLabelOf(this.model().type));
  readonly submitLabel = computed(() => this.isEdit()
    ? 'Zapisz zmiany'
    : this.typeOptions.find(option => option.type === this.model().type)!.verb);

  constructor() {
    effect(() => {
      const state = this.state();
      if (state.visible) {
        untracked(() => this.transactionForm().reset(this.initialModel(state)));
      }
    });
  }

  selectType(type: TransactionType): void {
    this.model.update(model => ({...model, type, itemId: null, shared: isSharedByDefault(type)}));
  }

  selectItem(itemId: number): void {
    this.transactionForm.itemId().value.set(itemId);
  }

  selectMember(memberId: number | null): void {
    this.transactionForm.memberId().value.set(memberId);
  }

  selectQuickDate(quickDate: QuickDate): void {
    this.transactionForm.date().value.set(addDays(this.calendar.today(), -quickDate.daysAgo));
  }

  isQuickDateSelected(quickDate: QuickDate): boolean {
    return isSameDay(this.model().date, addDays(this.calendar.today(), -quickDate.daysAgo));
  }

  focusAmount(): void {
    this.amountInput()?.input().nativeElement.focus();
  }

  onVisibleChange(visible: boolean): void {
    if (!visible) {
      this.editor.close();
    }
  }

  save(event: SubmitEvent): void {
    event.preventDefault();
    this.submit(false);
  }

  saveAndAddNext(): void {
    this.submit(true);
  }

  deleteTransaction(event: Event): void {
    this.deletion.confirmDeletion(this.state().editing!, event.currentTarget, () => this.editor.close());
  }

  private submit(addNext: boolean): void {
    if (!prepareFormSubmission(this.transactionForm, this.isWorking())) {
      return;
    }
    const request = toTransactionRequest(this.model());
    this.isWorking.set(true);
    this.transactionService.saveTransaction(request)
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          showSuccess(this.isEdit() ? 'Zapisano zmiany' : 'Dodano', this.describe(request), this.messageService);
          if (addNext) {
            this.transactionForm().reset({...this.model(), amount: null, note: ''});
            this.focusAmount();
          } else {
            this.editor.close();
          }
        },
        error: (error) => showServerError('Nie udało się zapisać transakcji', error, this.messageService),
      });
  }

  private describe(request: TransactionRequest): string {
    return transactionLabel(this.configStore.itemOf(request.type, request.itemId)?.name, request.amount);
  }

  private initialModel(state: TransactionEditorState): TransactionFormModel {
    return state.editing
      ? editTransactionModel(state.editing)
      : newTransactionModel(state.draft, this.defaultDate(), this.currentMemberId());
  }

  private defaultDate(): Date {
    const browsedMonth = this.editor.browsedMonth();
    return browsedMonth !== null && browsedMonth < this.calendar.currentMonth() ? endOfMonth(parseMonthKey(browsedMonth)) : this.calendar.today();
  }

  private pastMonthLabelOf(date: Date): string | null {
    const month = toMonthKey(date);
    return month < this.calendar.currentMonth() ? monthLabel(month) : null;
  }


  private selectableMembers(): Member[] {
    const members = this.householdStore.members();
    const editing = this.state().editing;
    const editedMember = editing ? this.householdStore.memberOf(editing.memberId) : undefined;
    return editedMember && !members.includes(editedMember) ? [...members, editedMember] : members;
  }

  private currentMemberId(): number | null {
    return this.authService.currentMember()?.id ?? null;
  }

  protected readonly AppRoute = AppRoute;
}
