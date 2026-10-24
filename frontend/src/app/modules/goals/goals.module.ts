import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {SharedModule} from '../../shared/shared.module';
import {GoalsComponent} from './goals.component';
import {GoalCardComponent} from './goal-card/goal-card.component';

const GOALS_ROUTES: Routes = [
  {path: '', component: GoalsComponent},
];

@NgModule({
  declarations: [
    GoalsComponent,
    GoalCardComponent,
  ],
  imports: [
    SharedModule,
    RouterModule.forChild(GOALS_ROUTES),
  ]
})
export class GoalsModule {
}
