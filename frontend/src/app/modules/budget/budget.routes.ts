import {Routes} from '@angular/router';
import {RoutePath} from '../../app-routes';
import {BudgetComponent} from './budget.component';
import {CurrentMonthComponent} from './current-month/current-month.component';
import {MonthHistoryComponent} from './month-history/month-history.component';
import {MonthDetailComponent} from './month-detail/month-detail.component';
import {pastMonthGuard} from './guard/past-month.guard';

export const BUDGET_ROUTES: Routes = [
  {
    path: '',
    component: BudgetComponent,
    children: [
      {path: '', component: CurrentMonthComponent},
      {path: RoutePath.HISTORY, component: MonthHistoryComponent},
      {path: `${RoutePath.HISTORY}/:${RoutePath.MONTH_PARAM}`, component: MonthDetailComponent, canActivate: [pastMonthGuard]},
    ],
  },
];
