import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MonthStore} from '../../stories/month-store';

@Component({
  selector: 'app-member-split',
  templateUrl: './member-split.component.html',
  styleUrl: './member-split.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberSplitComponent {
  readonly store = inject(MonthStore);
}
