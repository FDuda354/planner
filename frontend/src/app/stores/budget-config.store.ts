import {computed, inject, Injectable} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {BudgetConfigService} from '../services/budget-config.service';
import {BudgetConfig, BudgetItem} from '../models/budget-item.model';
import {TransactionType} from '../models/transaction.model';
import {AuthService} from '../modules/auth/services/auth.service';

const EMPTY_CONFIG: BudgetConfig = {expenseCategories: [], incomeSources: [], savingsGoals: []};

@Injectable({providedIn: 'root'})
export class BudgetConfigStore {
  private readonly configService = inject(BudgetConfigService);
  private readonly authService = inject(AuthService);

  readonly config = rxResource<BudgetConfig, number | undefined>({
    params: () => this.authService.currentMember()?.id,
    stream: () => this.configService.getBudgetConfig(),
    defaultValue: EMPTY_CONFIG,
  });

  readonly expenseCategories = computed(() => this.config.value().expenseCategories);
  readonly incomeSources = computed(() => this.config.value().incomeSources);
  readonly savingsGoals = computed(() => this.config.value().savingsGoals);

  private readonly itemsByType = computed<Record<TransactionType, BudgetItem[]>>(() => ({
    [TransactionType.EXPENSE]: this.expenseCategories(),
    [TransactionType.INCOME]: this.incomeSources(),
    [TransactionType.SAVING]: this.savingsGoals(),
  }));

  itemsOf(type: TransactionType): BudgetItem[] {
    return this.itemsByType()[type];
  }

  itemOf(type: TransactionType, itemId: number): BudgetItem | undefined {
    return this.itemsOf(type).find(item => item.id === itemId);
  }

  reload(): void {
    this.config.reload();
  }
}
