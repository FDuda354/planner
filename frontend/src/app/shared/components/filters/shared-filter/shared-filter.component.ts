import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';

@Component({
  selector: 'ui-shared-filter',
  templateUrl: './shared-filter.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedFilterComponent {
  readonly active = input.required<boolean>();
  readonly changed = output<boolean>();
}
