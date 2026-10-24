import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {SharedModule} from '../../shared/shared.module';
import {SummaryComponent} from './summary.component';
import {CashflowChartComponent} from './cashflow-chart/cashflow-chart.component';
import {IncomeStructureComponent} from './income-structure/income-structure.component';
import {CategoryHeatmapComponent} from './category-heatmap/category-heatmap.component';

const SUMMARY_ROUTES: Routes = [
  {path: '', component: SummaryComponent},
];

@NgModule({
  declarations: [
    SummaryComponent,
    CashflowChartComponent,
    IncomeStructureComponent,
    CategoryHeatmapComponent,
  ],
  imports: [
    SharedModule,
    RouterModule.forChild(SUMMARY_ROUTES),
  ]
})
export class SummaryModule {
}
