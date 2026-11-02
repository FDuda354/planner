import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BudgetConfig, BudgetItemRequest, ItemUsage, SavingsGoalRequest} from '../models/BudgetItem';
import {TransactionType} from '../models/Transaction';
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

  getItemUsage(type: TransactionType, itemId: number): Observable<ItemUsage> {
    return this.http.get<ItemUsage>(`${this.baseUrl}/item/usage`, {params: itemParams(type, itemId)});
  }

  deleteItem(type: TransactionType, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/item`, {params: itemParams(type, itemId)});
  }
}

function itemParams(type: TransactionType, itemId: number): HttpParams {
  return new HttpParams().set('type', type).set('itemId', itemId);
}
