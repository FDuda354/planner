import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {FieldTree} from '@angular/forms/signals';

@Component({
  selector: 'ui-field-error',
  templateUrl: './field-error.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldErrorComponent {
  readonly field = input.required<FieldTree<unknown>>();
}
