import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {AppRoute, RoutePath} from '../../../app-routes';
import {CalendarStore} from '../../../stores/calendar.store';
import {isMonthKey} from '../../../shared/utils/month';

export const pastMonthGuard: CanActivateFn = (route) => {
  const month = route.paramMap.get(RoutePath.MONTH_PARAM) ?? '';
  const isPastMonth = isMonthKey(month) && month < inject(CalendarStore).currentMonth();
  return isPastMonth || inject(Router).createUrlTree([AppRoute.BUDGET]);
};
