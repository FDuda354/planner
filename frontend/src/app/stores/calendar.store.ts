import {computed, Injectable, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {filter, fromEvent, interval, map, merge} from 'rxjs';
import {endOfMonth, isSameDay, startOfToday} from 'date-fns';
import {toDateKey, toMonthKey} from '../shared/utils/month';

const DAY_CHANGE_CHECK_MS = 60_000;

@Injectable({providedIn: 'root'})
export class CalendarStore {
  private readonly _today = signal(startOfToday());
  readonly today = this._today.asReadonly();
  readonly todayKey = computed(() => toDateKey(this._today()));
  readonly currentMonth = computed(() => toMonthKey(this._today()));
  readonly currentYear = computed(() => this._today().getFullYear());
  readonly lastDayOfMonth = computed(() => endOfMonth(this._today()));

  constructor() {
    merge(interval(DAY_CHANGE_CHECK_MS), fromEvent(document, 'visibilitychange'))
      .pipe(
        map(() => startOfToday()),
        filter(day => !isSameDay(day, this._today())),
        takeUntilDestroyed(),
      )
      .subscribe(day => this._today.set(day));
  }

  isAfterToday(dateKey: string): boolean {
    return dateKey > this.todayKey();
  }
}
