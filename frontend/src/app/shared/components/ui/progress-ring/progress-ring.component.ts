import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';

@Component({
  selector: 'ui-progress-ring',
  templateUrl: './progress-ring.component.html',
  styleUrl: './progress-ring.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProgressRingComponent {
  readonly progress = input.required<number>();
  readonly color = input.required<string>();
  readonly size = input(120);
  readonly stroke = input(10);

  readonly radius = computed(() => (this.size() - this.stroke()) / 2);
  readonly circumference = computed(() => 2 * Math.PI * this.radius());
  readonly dashOffset = computed(() => this.circumference() * (1 - Math.min(Math.max(this.progress(), 0), 1)));
}
