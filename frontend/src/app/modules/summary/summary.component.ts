import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {SummaryStore} from './stories/summary-store';

@Component({
  selector: 'app-summary',
  templateUrl: './summary.component.html',
  styleUrl: './summary.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [SummaryStore],
})
export class SummaryComponent {
  readonly store = inject(SummaryStore);
}
