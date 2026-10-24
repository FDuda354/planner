export const RoutePath = {
  BUDGET: 'budget',
  HISTORY: 'history',
  SUMMARY: 'summary',
  GOALS: 'goals',
  SETTINGS: 'settings',
  LOGIN: 'login',
  MONTH_PARAM: 'month',
} as const;

export const AppRoute = {
  BUDGET: `/${RoutePath.BUDGET}`,
  HISTORY: `/${RoutePath.BUDGET}/${RoutePath.HISTORY}`,
  SUMMARY: `/${RoutePath.SUMMARY}`,
  GOALS: `/${RoutePath.GOALS}`,
  SETTINGS: `/${RoutePath.SETTINGS}`,
  LOGIN: `/${RoutePath.LOGIN}`,
} as const;

export function monthDetailRoute(month: string): string {
  return `${AppRoute.HISTORY}/${month}`;
}
