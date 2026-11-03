import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {TRANSACTION_TYPE_FILTER_OPTIONS, TransactionType} from '../../../../models/transaction.model';

@Component({
  selector: 'ui-type-filter',
  templateUrl: './type-filter.component.html',
  styleUrl: './type-filter.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TypeFilterComponent {
  readonly value = input.required<TransactionType | null>();
  readonly countOf = input.required<(type: TransactionType | null) => number>();
  readonly name = input.required<string>();
  readonly changed = output<TransactionType | null>();

  readonly options = TRANSACTION_TYPE_FILTER_OPTIONS;
}
