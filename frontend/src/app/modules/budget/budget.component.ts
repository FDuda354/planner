import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {toSignal} from '@angular/core/rxjs-interop';
import {filter, map} from 'rxjs';
import {AppRoute} from '../../app-routes';

@Component({
  selector: 'app-budget',
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BudgetComponent {
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    {initialValue: this.router.url},
  );

  readonly activeTab = computed(() => this.url().startsWith(AppRoute.HISTORY) ? AppRoute.HISTORY : AppRoute.BUDGET);

  protected readonly AppRoute = AppRoute;
}
