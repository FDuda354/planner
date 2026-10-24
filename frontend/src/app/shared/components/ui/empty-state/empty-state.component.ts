import {ChangeDetectionStrategy, Component, input} from '@angular/core';

@Component({
  selector: 'ui-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly icon = input('pi-inbox');
  readonly title = input.required<string>();
  readonly text = input('');
}
