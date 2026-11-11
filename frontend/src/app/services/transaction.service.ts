import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable, signal} from '@angular/core';
import {Observable, tap} from 'rxjs';
import {Transaction, TransactionRequest} from '../models/transaction.model';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.api.baseUrl}/api/transaction`;
  private readonly _revision = signal(0);
  readonly revision = this._revision.asReadonly();

  getTransactions(month: string): Observable<Transaction[]> {
    const params = new HttpParams().set('month', month);
    return this.http.get<Transaction[]>(this.baseUrl, {params});
  }

  getYearTransactions(year: number): Observable<Transaction[]> {
    const params = new HttpParams().set('year', year);
    return this.http.get<Transaction[]>(`${this.baseUrl}/year`, {params});
  }

  saveTransaction(request: TransactionRequest): Observable<void> {
    return this.http.post<void>(this.baseUrl, request).pipe(tap(() => this.markChanged()));
  }

  deleteTransaction(id: number): Observable<void> {
    const params = new HttpParams().set('transactionId', id);
    return this.http.delete<void>(this.baseUrl, {params}).pipe(tap(() => this.markChanged()));
  }

  markChanged(): void {
    this._revision.update(revision => revision + 1);
  }
}
