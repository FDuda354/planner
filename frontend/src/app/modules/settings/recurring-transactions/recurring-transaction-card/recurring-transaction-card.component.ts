import {ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal} from '@angular/core';
import {form} from '@angular/forms/signals';
import {ConfirmationService, MessageService} from 'primeng/api';
import {RecurringTransactionsStore} from '../../stories/recurring-transactions-store';
import {RecurringTransactionService} from '../../../../services/recurring-transaction.service';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {CalendarStore} from '../../../../stores/calendar.store';
import {parseDateKey} from '../../../../shared/utils/month';
import {RecurrenceFrequency, RecurringTransaction} from '../../../../models/RecurringTransaction';
import {annualDateOf, SHORTEST_MONTH_LENGTH} from '../models/recurring-schedule';
import {format} from 'date-fns';
import {pl} from 'date-fns/locale';
import {TransactionType} from '../../../../models/Transaction';
import {editRecurringModel, toRecurringRequest} from '../models/form-recurring-transaction';
import {showServerError, showSuccess} from '../../../../utils/NotificationHelper';

function scheduleLabel(recurring: RecurringTransaction): string {
  if (recurring.frequency === RecurrenceFrequency.YEARLY) {
    return `Co roku, ${format(annualDateOf(recurring.monthOfYear!, recurring.dayOfMonth, new Date().getFullYear()), 'd MMMM', {locale: pl})}`;
  }
  const lastDayHint = recurring.dayOfMonth > SHORTEST_MONTH_LENGTH ? ' (w krótszych miesiącach ostatniego dnia)' : '';
  return `Co miesiąc, ${recurring.dayOfMonth}. dnia${lastDayHint}`;
}

const AMOUNT_SIGNS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: '−',
  [TransactionType.INCOME]: '+',
  [TransactionType.SAVING]: '',
};

@Component({
  selector: 'app-recurring-transaction-card',
  templateUrl: './recurring-transaction-card.component.html',
  styleUrl: './recurring-transaction-card.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecurringTransactionCardComponent {
  private readonly store = inject(RecurringTransactionsStore);
  private readonly recurringService = inject(RecurringTransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly householdStore = inject(HouseholdStore);
  private readonly calendar = inject(CalendarStore);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  readonly recurring = input.required<RecurringTransaction>();

  readonly activeForm = form(linkedSignal(() => ({active: this.recurring().active})));
  readonly item = computed(() => this.configStore.itemOf(this.recurring().type, this.recurring().itemId));
  readonly itemName = computed(() => this.item()?.name ?? '');
  readonly member = computed(() => this.householdStore.memberOf(this.recurring().memberId));
  readonly sign = computed(() => AMOUNT_SIGNS[this.recurring().type]);
  readonly schedule = computed(() => scheduleLabel(this.recurring()));
  readonly isYearly = computed(() => this.recurring().frequency === RecurrenceFrequency.YEARLY);
  readonly nextOccurrenceFormat = computed(() => this.isNextOccurrenceInAnotherYear() ? 'd MMMM y' : 'd MMMM');

  private isNextOccurrenceInAnotherYear(): boolean {
    return parseDateKey(this.recurring().nextOccurrence).getFullYear() !== this.calendar.currentYear();
  }

  edit(): void {
    this.store.edit(this.recurring());
  }

  toggleActive(): void {
    const request = toRecurringRequest({...editRecurringModel(this.recurring()), active: !this.recurring().active});
    this.recurringService.saveRecurringTransaction(request).subscribe({
      next: () => {
        showSuccess(request.active ? 'Wznowiono' : 'Wstrzymano', this.itemName(), this.messageService);
        this.store.reload();
      },
      error: (error) => showServerError('Nie udało się zmienić statusu', error, this.messageService),
    });
  }

  delete(): void {
    const recurring = this.recurring();
    const name = this.itemName();
    this.confirmationService.confirm({
      key: 'deleteConfirmDialog',
      header: 'Usunąć stałą transakcję?',
      message: `„${name}” przestanie się automatycznie księgować. Transakcje, które już się pojawiły, zostaną w historii.`,
      icon: 'pi pi-trash',
      acceptLabel: 'Usuń',
      rejectLabel: 'Anuluj',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.recurringService.deleteRecurringTransaction(recurring.id).subscribe({
        next: () => {
          showSuccess('Usunięto stałą transakcję', name, this.messageService);
          this.store.reload();
        },
        error: (error) => showServerError('Nie udało się usunąć', error, this.messageService),
      }),
    });
  }
}
