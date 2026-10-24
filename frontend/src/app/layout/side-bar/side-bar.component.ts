import {ChangeDetectionStrategy, Component, inject, input} from '@angular/core';
import {MenuItem} from 'primeng/api';
import {HouseholdStore} from '../../stores/household.store';
import {AppRoute} from '../../app-routes';

@Component({
  selector: 'app-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrl: './side-bar.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SideBarComponent {
  readonly householdStore = inject(HouseholdStore);

  readonly menuItems = input.required<MenuItem[]>();

  protected readonly AppRoute = AppRoute;
}
