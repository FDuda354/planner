import {ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked, viewChild} from '@angular/core';
import {apply, disabled, form} from '@angular/forms/signals';
import {ConfirmationService, MessageService} from 'primeng/api';
import {InputNumber} from 'primeng/inputnumber';
import {finalize} from 'rxjs';
import {addDays, endOfMonth, isSameDay} from 'date-fns';
import {TransactionService} from '../../../services/transaction.service';
import {TransactionEditorState, TransactionEditorStore} from '../../../stores/transaction-editor.store';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {CalendarStore} from '../../../stores/calendar.store';
import {TRANSACTION_TYPE_OPTIONS, TransactionRequest, TransactionType} from '../../../models/Transaction';
import {BudgetItem} from '../../../models/BudgetItem';
import {monthLabel, parseMonthKey, toMonthKey} from '../../utils/month';
import {prepareFormSubmission} from '../../forms/prepare-form-submission';
import {showServerError, showSuccess} from '../../../utils/NotificationHelper';
import {formatMoney} from '../../utils/money';
import {AppRoute} from '../../../app-routes';
import {editTransactionModel, newTransactionModel, toTransactionRequest, TransactionFormModel, transactionSchema} from './models/form-transaction';

interface QuickDate {
  label: string;
  daysAgo: number;
}

const ITEM_LABELS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: 'Na co',
  [TransactionType.INCOME]: 'Skąd',
  [TransactionType.SAVING]: 'Na jaki cel',
};

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
  private readonly confirmationService = inject(ConfirmationService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly calendar = inject(CalendarStore);
  private readonly amountInput = viewChild(InputNumber);

  readonly typeOptions = TRANSACTION_TYPE_OPTIONS;
  readonly sharedInputId = 'transaction-shared';
  readonly quickDates: QuickDate[] = [
    {label: 'Dziś', daysAgo: 0},
    {label: 'Wczoraj', daysAgo: 1},
    {label: 'Przedwczoraj', daysAgo: 2},
  ];

  readonly today = this.calendar.today;
  readonly state = this.editor.state;
  readonly isEdit = computed(() => this.state().editing !== null);
  readonly isWorking = signal(false);
  readonly showErrors = signal(false);

  readonly model = signal<TransactionFormModel>(newTransactionModel(this.state().draft, this.calendar.today()));
  readonly transactionForm = form(this.model, (path) => {
    apply(path, transactionSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  readonly items = computed(() => this.selectableItems(this.model().type));
  readonly isExpense = computed(() => this.model().type === TransactionType.EXPENSE);
  readonly pastMonthLabel = computed(() => this.pastMonthLabelOf(this.model().date));
  readonly itemLabel = computed(() => ITEM_LABELS[this.model().type]);
  readonly submitLabel = computed(() => this.isEdit()
    ? 'Zapisz zmiany'
    : this.typeOptions.find(option => option.type === this.model().type)!.verb);

  constructor() {
    effect(() => {
      const state = this.state();
      if (state.visible) {
        untracked(() => {
          this.showErrors.set(false);
          this.transactionForm().reset(this.initialModel(state));
        });
      }
    });
  }

  selectType(type: TransactionType): void {
    this.model.update(model => ({...model, type, itemId: null, shared: true}));
  }

  selectItem(itemId: number): void {
    this.transactionForm.itemId().value.set(itemId);
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
    const transaction = this.state().editing!;
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: 'Na pewno usunąć tę transakcję?',
      icon: 'pi pi-trash',
      acceptLabel: 'Usuń',
      rejectLabel: 'Anuluj',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-text p-button-sm',
      accept: () => {
        this.isWorking.set(true);
        this.transactionService.deleteTransaction(transaction.id)
          .pipe(finalize(() => this.isWorking.set(false)))
          .subscribe({
            next: () => {
              showSuccess('Usunięto transakcję', this.describe(transaction), this.messageService);
              this.editor.close();
            },
            error: (error) => showServerError('Nie udało się usunąć transakcji', error, this.messageService),
          });
      },
    });
  }

  private submit(addNext: boolean): void {
    this.showErrors.set(true);
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
            this.showErrors.set(false);
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
    const item = this.configStore.itemOf(request.type, request.itemId);
    return `${item?.name ?? ''} · ${formatMoney(request.amount)}`;
  }

  private initialModel(state: TransactionEditorState): TransactionFormModel {
    return state.editing
      ? editTransactionModel(state.editing)
      : newTransactionModel(state.draft, this.defaultDate());
  }

  private defaultDate(): Date {
    const browsedMonth = this.editor.browsedMonth();
    return browsedMonth !== null && browsedMonth < this.calendar.currentMonth() ? endOfMonth(parseMonthKey(browsedMonth)) : this.calendar.today();
  }

  private pastMonthLabelOf(date: Date): string | null {
    const month = toMonthKey(date);
    return month < this.calendar.currentMonth() ? monthLabel(month) : null;
  }

  private selectableItems(type: TransactionType): BudgetItem[] {
    const editing = this.state().editing;
    const editedItemId = editing?.type === type ? editing.itemId : null;
    return this.configStore.itemsOf(type).filter(item => item.active || item.id === editedItemId);
  }

  protected readonly AppRoute = AppRoute;
}
