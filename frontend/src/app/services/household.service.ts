import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {Household} from '../models/Member';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class HouseholdService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.api.baseUrl}/api/household`;

  getHousehold(): Observable<Household> {
    return this.http.get<Household>(this.baseUrl);
  }

  inviteMember(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/invitation`, {email});
  }

  cancelInvitation(email: string): Observable<void> {
    const params = new HttpParams().set('email', email);
    return this.http.delete<void>(`${this.baseUrl}/invitation`, {params});
  }

  removeMember(memberId: number): Observable<void> {
    const params = new HttpParams().set('memberId', memberId);
    return this.http.delete<void>(`${this.baseUrl}/member`, {params});
  }

  restoreMember(memberId: number): Observable<void> {
    const params = new HttpParams().set('memberId', memberId);
    return this.http.patch<void>(`${this.baseUrl}/member/restore`, null, {params});
  }
}
