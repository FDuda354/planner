import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {RecurringTransactionsStore} from '../stories/recurring-transactions-store';

@Component({
  selector: 'app-recurring-transactions',
  templateUrl: './recurring-transactions.component.html',
  styleUrl: './recurring-transactions.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [RecurringTransactionsStore],
})
export class RecurringTransactionsComponent {
  readonly store = inject(RecurringTransactionsStore);
}
