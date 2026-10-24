import {computed, inject, Injectable, linkedSignal} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {SummaryService} from '../../../services/summary.service';
import {TransactionService} from '../../../services/transaction.service';
import {MonthSummary} from '../../../models/Summary';
import {monthsOfYear, yearOfMonth} from '../../../shared/utils/month';
import {CalendarStore} from '../../../stores/calendar.store';
import {reloadWhenChanged} from '../../../shared/utils/resource';

@Injectable()
export class HistoryStore {
  private readonly summaryService = inject(SummaryService);
  private readonly transactionService = inject(TransactionService);
  private readonly calendar = inject(CalendarStore);

  readonly data = rxResource<MonthSummary[], void>({
    stream: () => this.summaryService.getMonthSummaries(),
    defaultValue: [],
  });

  private readonly pastMonths = computed(() => this.data.value().filter(summary => summary.month < this.calendar.currentMonth()));
  readonly years = computed(() => [...new Set(this.pastMonths().map(summary => yearOfMonth(summary.month)))]);
  readonly year = linkedSignal(() => this.years()[0] ?? this.calendar.currentYear());

  readonly monthsOfYear = computed(() => this.pastMonths().filter(summary => yearOfMonth(summary.month) === this.year()));
  readonly calendarMonths = computed(() => monthsOfYear(this.year()));
  readonly summaryByMonth = computed(() => new Map(this.monthsOfYear().map(summary => [summary.month, summary])));
  readonly hasHistory = computed(() => this.pastMonths().length > 0);

  constructor() {
    reloadWhenChanged(this.transactionService.revision, this.data);
  }

  selectYear(year: number): void {
    this.year.set(year);
  }
}
