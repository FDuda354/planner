import {HttpClient} from '@angular/common/http';
import {inject, Injectable, signal} from '@angular/core';
import {Router} from '@angular/router';
import {catchError, map, Observable, of, tap} from 'rxjs';
import {AuthRequest, Member, RegisterRequest} from '../../../models/Member';
import {environment} from '../../../../environments/environment';
import {AppRoute} from '../../../app-routes';

@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.api.baseUrl}/api/auth`;

  private readonly _currentMember = signal<Member | null>(null);
  readonly currentMember = this._currentMember.asReadonly();

  login(request: AuthRequest): Observable<Member> {
    return this.http.post<Member>(`${this.baseUrl}/login`, request).pipe(
      tap(member => this._currentMember.set(member))
    );
  }

  register(request: RegisterRequest): Observable<Member> {
    return this.http.post<Member>(`${this.baseUrl}/register`, request).pipe(
      tap(member => this._currentMember.set(member))
    );
  }

  loadMe(): Observable<Member | null> {
    const cached = this._currentMember();
    if (cached) return of(cached);

    return this.http.get<Member | null>(`${this.baseUrl}/me`).pipe(
      map(member => {
        this._currentMember.set(member);
        return member;
      }),
      catchError(() => {
        this._currentMember.set(null);
        return of<Member | null>(null);
      })
    );
  }

  clear(): void {
    this._currentMember.set(null);
  }

  logout(): void {
    this.http.post<void>(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => {
        this.clear();
        void this.router.navigate([AppRoute.LOGIN]);
      },
    });
  }
}
