import {inject, Injectable} from '@angular/core';
import {Observable, tap} from 'rxjs';
import {BudgetConfigService} from '../../../services/budget-config.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {TransactionType} from '../../../models/Transaction';
import {BudgetItemFormModel, toBudgetItemRequest, toSavingsGoalRequest} from '../budget-item-editor/models/form-budget-item';

@Injectable({providedIn: 'root'})
export class BudgetItemSaverService {
  private readonly configService = inject(BudgetConfigService);
  private readonly configStore = inject(BudgetConfigStore);

  save(kind: TransactionType, model: BudgetItemFormModel): Observable<void> {
    return this.requestFor(kind, model).pipe(tap(() => this.configStore.reload()));
  }

  private requestFor(kind: TransactionType, model: BudgetItemFormModel): Observable<void> {
    switch (kind) {
      case TransactionType.EXPENSE:
        return this.configService.saveExpenseCategory(toBudgetItemRequest(model));
      case TransactionType.INCOME:
        return this.configService.saveIncomeSource(toBudgetItemRequest(model));
      case TransactionType.SAVING:
        return this.configService.saveSavingsGoal(toSavingsGoalRequest(model));
    }
  }
}
