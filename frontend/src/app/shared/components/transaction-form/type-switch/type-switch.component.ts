import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {TRANSACTION_TYPE_OPTIONS, TransactionType} from '../../../../models/transaction.model';

@Component({
  selector: 'ui-type-switch',
  templateUrl: './type-switch.component.html',
  styleUrl: './type-switch.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {'[attr.data-transaction-type]': 'value()'},
})
export class TypeSwitchComponent {
  readonly value = input.required<TransactionType>();
  readonly name = input.required<string>();
  readonly changed = output<TransactionType>();

  readonly options = TRANSACTION_TYPE_OPTIONS;
}
