import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {DefaultComponent} from './layout/default/default-component';
import {authGuard} from './modules/auth/guard/auth.guard';
import {guestGuard} from './modules/auth/guard/guest.guard';
import {LoginComponent} from './modules/auth/views/login/login.component';
import {RoutePath} from './app-routes';

const routes: Routes = [
  {
    path: '',
    component: DefaultComponent,
    canActivate: [authGuard],
    children: [
      {path: '', pathMatch: 'full', redirectTo: RoutePath.BUDGET},
      {
        path: RoutePath.BUDGET,
        loadChildren: () =>
          import('./modules/budget/budget.module').then((m) => m.BudgetModule),
      },
      {
        path: RoutePath.SUMMARY,
        loadChildren: () =>
          import('./modules/summary/summary.module').then((m) => m.SummaryModule),
      },
      {
        path: RoutePath.GOALS,
        loadChildren: () =>
          import('./modules/goals/goals.module').then((m) => m.GoalsModule),
      },
      {
        path: RoutePath.SETTINGS,
        loadChildren: () =>
          import('./modules/settings/settings.module').then((m) => m.SettingsModule),
      },
    ],
  },
  {
    path: RoutePath.LOGIN, component: LoginComponent, canActivate: [guestGuard],
  },
  {path: '**', redirectTo: ''},
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {bindToComponentInputs: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
