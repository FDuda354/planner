import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {map} from 'rxjs/operators';
import {AuthService} from '../services/auth.service';
import {AppRoute} from '../../../app-routes';

export const authGuard: CanActivateFn = (_route, _state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.loadMe().pipe(
    map(member => member ? true : router.createUrlTree([AppRoute.LOGIN]))
  );
};
