import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {SharedModule} from '../../shared/shared.module';
import {BUDGET_ROUTES} from './budget.routes';
import {BudgetComponent} from './budget.component';
import {CurrentMonthComponent} from './current-month/current-month.component';
import {MonthHistoryComponent} from './month-history/month-history.component';
import {MonthCardComponent} from './month-history/month-card/month-card.component';
import {MonthDetailComponent} from './month-detail/month-detail.component';
import {MonthOverviewComponent} from './month-overview/month-overview.component';
import {MonthBalanceComponent} from './month-overview/month-balance/month-balance.component';
import {MemberSplitComponent} from './month-overview/member-split/member-split.component';
import {CategoryBreakdownComponent} from './month-overview/category-breakdown/category-breakdown.component';
import {SpendingPaceComponent} from './month-overview/spending-pace/spending-pace.component';
import {TransactionFeedComponent} from './month-overview/transaction-feed/transaction-feed.component';

@NgModule({
  declarations: [
    BudgetComponent,
    CurrentMonthComponent,
    MonthHistoryComponent,
    MonthCardComponent,
    MonthDetailComponent,
    MonthOverviewComponent,
    MonthBalanceComponent,
    MemberSplitComponent,
    CategoryBreakdownComponent,
    SpendingPaceComponent,
    TransactionFeedComponent,
  ],
  imports: [
    SharedModule,
    RouterModule.forChild(BUDGET_ROUTES),
  ]
})
export class BudgetModule {
}
