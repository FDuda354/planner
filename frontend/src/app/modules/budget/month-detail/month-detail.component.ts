import {ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input} from '@angular/core';
import {AppRoute, monthDetailRoute} from '../../../app-routes';
import {monthLabel, shiftMonth} from '../../../shared/utils/month';
import {TransactionEditorStore} from '../../../stores/transaction-editor.store';
import {CalendarStore} from '../../../stores/calendar.store';

@Component({
  selector: 'app-month-detail',
  templateUrl: './month-detail.component.html',
  styleUrl: './month-detail.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthDetailComponent {
  private readonly editor = inject(TransactionEditorStore);
  private readonly calendar = inject(CalendarStore);

  readonly month = input.required<string>();

  readonly previousRoute = computed(() => monthDetailRoute(shiftMonth(this.month(), -1)));
  readonly previousLabel = computed(() => monthLabel(shiftMonth(this.month(), -1)));
  readonly nextMonth = computed(() => shiftMonth(this.month(), 1));
  readonly nextLabel = computed(() => monthLabel(this.nextMonth()));
  readonly nextRoute = computed(() => this.nextMonth() === this.calendar.currentMonth() ? AppRoute.BUDGET : monthDetailRoute(this.nextMonth()));

  constructor() {
    effect(() => this.editor.browseMonth(this.month()));
    inject(DestroyRef).onDestroy(() => this.editor.browseMonth(null));
  }

  protected readonly AppRoute = AppRoute;
}
