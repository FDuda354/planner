import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {BudgetItem} from '../../../../models/budget-item.model';

@Component({
  selector: 'ui-item-picker',
  templateUrl: './item-picker.component.html',
  styleUrl: './item-picker.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemPickerComponent {
  readonly items = input.required<BudgetItem[]>();
  readonly value = input.required<number | null>();
  readonly label = input.required<string>();
  readonly changed = output<number>();
}
