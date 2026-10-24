import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {GoalProgress, MonthSummary} from '../models/Summary';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class SummaryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.api.baseUrl}/api/summary`;

  getMonthSummaries(): Observable<MonthSummary[]> {
    return this.http.get<MonthSummary[]>(`${this.baseUrl}/months`);
  }

  getYearSummary(year: number): Observable<MonthSummary[]> {
    const params = new HttpParams().set('year', year);
    return this.http.get<MonthSummary[]>(`${this.baseUrl}/year`, {params});
  }

  getGoalProgress(): Observable<GoalProgress[]> {
    return this.http.get<GoalProgress[]>(`${this.baseUrl}/goals`);
  }
}
