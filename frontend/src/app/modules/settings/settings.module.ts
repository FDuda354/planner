import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {SharedModule} from '../../shared/shared.module';
import {SettingsComponent} from './settings.component';
import {BudgetItemListComponent} from './budget-item-list/budget-item-list.component';
import {BudgetItemCardComponent} from './budget-item-list/budget-item-card/budget-item-card.component';
import {BudgetItemEditorComponent} from './budget-item-editor/budget-item-editor.component';
import {HouseholdMembersComponent} from './household-members/household-members.component';
import {AccountSettingsComponent} from './account-settings/account-settings.component';
import {RecurringTransactionsComponent} from './recurring-transactions/recurring-transactions.component';
import {RecurringTransactionCardComponent} from './recurring-transactions/recurring-transaction-card/recurring-transaction-card.component';
import {RecurringTransactionEditorComponent} from './recurring-transactions/recurring-transaction-editor/recurring-transaction-editor.component';

const SETTINGS_ROUTES: Routes = [
  {path: '', component: SettingsComponent},
];

@NgModule({
  declarations: [
    SettingsComponent,
    BudgetItemListComponent,
    BudgetItemCardComponent,
    BudgetItemEditorComponent,
    HouseholdMembersComponent,
    AccountSettingsComponent,
    RecurringTransactionsComponent,
    RecurringTransactionCardComponent,
    RecurringTransactionEditorComponent,
  ],
  imports: [
    SharedModule,
    RouterModule.forChild(SETTINGS_ROUTES),
  ]
})
export class SettingsModule {
}
