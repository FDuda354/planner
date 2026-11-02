import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable, tap} from 'rxjs';
import {RecurringTransaction, RecurringTransactionRequest} from '../models/RecurringTransaction';
import {TransactionService} from './transaction.service';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class RecurringTransactionService {
  private readonly http = inject(HttpClient);
  private readonly transactionService = inject(TransactionService);
  private readonly baseUrl = `${environment.api.baseUrl}/api/recurring-transaction`;

  getRecurringTransactions(): Observable<RecurringTransaction[]> {
    return this.http.get<RecurringTransaction[]>(this.baseUrl);
  }

  saveRecurringTransaction(request: RecurringTransactionRequest): Observable<void> {
    return this.http.post<void>(this.baseUrl, request).pipe(tap(() => this.transactionService.markChanged()));
  }

  deleteRecurringTransaction(id: number): Observable<void> {
    const params = new HttpParams().set('recurringTransactionId', id);
    return this.http.delete<void>(this.baseUrl, {params});
  }
}
