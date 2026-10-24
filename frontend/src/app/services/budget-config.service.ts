import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BudgetConfig, BudgetItemRequest, SavingsGoalRequest} from '../models/BudgetItem';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class BudgetConfigService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.api.baseUrl}/api/budget-config`;

  getBudgetConfig(): Observable<BudgetConfig> {
    return this.http.get<BudgetConfig>(this.baseUrl);
  }

  saveExpenseCategory(request: BudgetItemRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/expense-category`, request);
  }

  saveIncomeSource(request: BudgetItemRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/income-source`, request);
  }

  saveSavingsGoal(request: SavingsGoalRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/savings-goal`, request);
  }
}
