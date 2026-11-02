import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

@Injectable({providedIn: 'root'})
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.api.baseUrl}/api/account`;

  deleteAccount(password: string): Observable<void> {
    return this.http.delete<void>(this.baseUrl, {body: {password}});
  }

  deleteAccountWithHousehold(password: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/with-household`, {body: {password}});
  }
}
