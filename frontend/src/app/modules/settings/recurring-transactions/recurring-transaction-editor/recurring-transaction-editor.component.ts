import {ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked} from '@angular/core';
import {apply, disabled, form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {finalize} from 'rxjs';
import {isSameDay} from 'date-fns';
import {RecurringEditorState, RecurringTransactionsStore} from '../../stores/recurring-transactions.store';
import {RecurringTransactionService} from '../../../../services/recurring-transaction.service';
import {BudgetConfigStore} from '../../../../stores/budget-config.store';
import {HouseholdStore} from '../../../../stores/household.store';
import {CalendarStore} from '../../../../stores/calendar.store';
import {AuthService} from '../../../auth/services/auth.service';
import {belongsToNobody, isSharedByDefault, itemLabelOf, TransactionType} from '../../../../models/transaction.model';
import {prepareFormSubmission} from '../../../../shared/forms/prepare-form-submission';
import {showServerError, showSuccess} from '../../../../shared/utils/notifications';
import {parseDateKey} from '../../../../shared/utils/month';
import {editRecurringModel, newRecurringModel, RecurringFormModel, recurringSchema, scheduleOf, toRecurringRequest} from '../models/form-recurring-transaction';
import {maxDayOf, MONTH_OPTIONS, nextOccurrenceFrom, RecurrenceSchedule, SHORTEST_MONTH_LENGTH} from '../models/recurring-schedule';
import {RECURRENCE_FREQUENCY_OPTIONS, RecurrenceFrequency, RecurringTransaction} from '../../../../models/recurring-transaction.model';

const FEBRUARY = 2;

function keepsSchedule(recurring: RecurringTransaction, schedule: RecurrenceSchedule): boolean {
  return recurring.frequency === schedule.frequency
         && recurring.dayOfMonth === schedule.dayOfMonth
         && recurring.monthOfYear === schedule.monthOfYear;
}

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
  private readonly calendar = inject(CalendarStore);
  private readonly messageService = inject(MessageService);
  readonly householdStore = inject(HouseholdStore);

  readonly frequencyOptions = RECURRENCE_FREQUENCY_OPTIONS;
  readonly monthOptions = MONTH_OPTIONS;
  readonly state = this.store.editor;
  readonly isEdit = computed(() => this.state().recurring !== null);
  readonly isWorking = signal(false);

  readonly model = signal<RecurringFormModel>(newRecurringModel(null));
  readonly recurringForm = form(this.model, (path) => {
    apply(path, recurringSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  readonly items = computed(() => this.configStore.itemsOf(this.model().type));
  readonly itemLabel = computed(() => itemLabelOf(this.model().type));
  readonly isAssignedToMember = computed(() => !belongsToNobody(this.model()));
  readonly nextOccurrence = computed(() => this.predictNextOccurrence(this.model()));
  readonly isBookedToday = computed(() => this.isToday(this.nextOccurrence()));
  readonly shortPeriodHint = computed(() => this.shortPeriodHintOf(scheduleOf(this.model())));
  readonly isYearly = computed(() => this.model().frequency === RecurrenceFrequency.YEARLY);
  readonly maxDayOfMonth = computed(() => this.isYearly() ? maxDayOf(this.model().monthOfYear) : 31);

  constructor() {
    effect(() => {
      const state = this.state();
      if (state.visible) {
        untracked(() => this.recurringForm().reset(this.initialModel(state)));
      }
    });
  }

  selectType(type: TransactionType): void {
    this.model.update(model => ({...model, type, itemId: null, shared: isSharedByDefault(type)}));
  }

  selectItem(itemId: number): void {
    this.recurringForm.itemId().value.set(itemId);
  }

  selectFrequency(frequency: RecurrenceFrequency): void {
    this.recurringForm.frequency().value.set(frequency);
  }

  selectMember(memberId: number | null): void {
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

  private isToday(date: Date | null): boolean {
    return date !== null && isSameDay(date, this.calendar.today());
  }

  private predictNextOccurrence(model: RecurringFormModel): Date | null {
    const schedule = scheduleOf(model);
    if (!model.active || schedule === null) {
      return null;
    }
    const editing = this.state().recurring;
    const scheduled = editing ? parseDateKey(editing.nextOccurrence) : null;
    return editing !== null && editing.active && keepsSchedule(editing, schedule)
      ? scheduled
      : nextOccurrenceFrom(schedule, this.calendar.today(), scheduled);
  }

  private shortPeriodHintOf(schedule: RecurrenceSchedule | null): string | null {
    if (schedule === null || schedule.dayOfMonth <= SHORTEST_MONTH_LENGTH) {
      return null;
    }
    if (schedule.frequency === RecurrenceFrequency.MONTHLY) {
      return `W miesiącach krótszych niż ${schedule.dayOfMonth} dni zaksięguje się ostatniego dnia miesiąca (np. 28 lub 29 lutego).`;
    }
    return schedule.monthOfYear === FEBRUARY ? 'W latach nieprzestępnych zaksięguje się 28 lutego.' : null;
  }

}
