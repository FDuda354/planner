import {ChangeDetectionStrategy, Component, inject, input} from '@angular/core';
import {CalendarStore} from '../../../stores/calendar.store';

@Component({
  selector: 'app-current-month',
  templateUrl: './current-month.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CurrentMonthComponent {
  readonly month = inject(CalendarStore).currentMonth;
  readonly category = input<string>();
}
