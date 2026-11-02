import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {FieldTree} from '@angular/forms/signals';
import {sharedFlagCopyOf, TransactionType} from '../../../../models/transaction.model';

@Component({
  selector: 'ui-shared-toggle',
  templateUrl: './shared-toggle.component.html',
  styleUrl: './shared-toggle.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedToggleComponent {
  readonly field = input.required<FieldTree<boolean>>();
  readonly type = input.required<TransactionType>();
  readonly inputId = input.required<string>();

  readonly copy = computed(() => sharedFlagCopyOf(this.type()));
}
