import {ChangeDetectionStrategy, Component, input} from '@angular/core';

export type ItemIconSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'ui-item-icon',
  templateUrl: './item-icon.component.html',
  styleUrl: './item-icon.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemIconComponent {
  readonly icon = input.required<string>();
  readonly color = input.required<string>();
  readonly size = input<ItemIconSize>('md');
}
