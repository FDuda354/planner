import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {GoalsStore} from './stories/goals-store';
import {AppRoute} from '../../app-routes';
import {HouseholdStore} from '../../stores/household.store';

@Component({
  selector: 'app-goals',
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [GoalsStore],
})
export class GoalsComponent {
  readonly store = inject(GoalsStore);
  readonly householdStore = inject(HouseholdStore);

  protected readonly AppRoute = AppRoute;
}
