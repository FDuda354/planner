import {HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest} from '@angular/common/http';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AuthService} from './auth.service';
import {AppRoute} from '../../../app-routes';

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

const LOGIN_PATH = '/api/auth/login';
const ME_PATH = '/api/auth/me';
const REGISTER_PATH = '/api/auth/register';

export const httpInterceptorFn: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const withCreds = req.clone({withCredentials: true});
  const outgoing = attachCsrfHeader(withCreds);

  return next(outgoing).pipe(
    catchError(err => {
      const isAuthError = err?.status === 401 || err?.status === 403;
      const isAuthEndpoint =
        req.url.endsWith(LOGIN_PATH) || req.url.endsWith(ME_PATH) || req.url.endsWith(REGISTER_PATH);

      if (isAuthError && !isAuthEndpoint) {
        authService.clear();
        void router.navigate([AppRoute.LOGIN]);
      }
      return throwError(() => err);
    })
  );
};

function attachCsrfHeader(req: HttpRequest<unknown>): HttpRequest<unknown> {
  if (!isMutating(req.method) || req.headers.has(CSRF_HEADER)) {
    return req;
  }
  const token = readCookie(CSRF_COOKIE);
  if (!token) return req;
  return req.clone({setHeaders: {[CSRF_HEADER]: token}});
}

function isMutating(method: string): boolean {
  return method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS';
}

function readCookie(name: string): string | null {
  const match = new RegExp(String.raw`(?:^|;\s*)` + name + '=([^;]*)').exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : null;
}
