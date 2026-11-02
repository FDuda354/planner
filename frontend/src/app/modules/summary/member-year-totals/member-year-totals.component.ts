import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {MemberYearTotal} from '../stores/summary.store';

@Component({
  selector: 'app-member-year-totals',
  templateUrl: './member-year-totals.component.html',
  styleUrl: './member-year-totals.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberYearTotalsComponent {
  readonly title = input.required<string>();
  readonly totals = input.required<MemberYearTotal[]>();
  readonly sharedLabel = input.required<string>();
  readonly sharedAmount = input.required<number>();
}
