import {ChartOptions} from 'chart.js';
import {formatMoney} from './money';

type TooltipConfig = NonNullable<NonNullable<ChartOptions['plugins']>['tooltip']>;

export interface ChartTheme {
  text: string;
  grid: string;
  surface: string;
  income: string;
  expense: string;
  saving: string;
  muted: string;
}

const LIGHT_THEME: ChartTheme = {
  text: '#6b7a75',
  grid: '#e9eeec',
  surface: '#ffffff',
  income: '#059669',
  expense: '#e11d48',
  saving: '#0284c7',
  muted: '#cfd7d4',
};

const DARK_THEME: ChartTheme = {
  text: '#8d9b96',
  grid: '#1f2b2e',
  surface: '#131c1f',
  income: '#34d399',
  expense: '#fb7185',
  saving: '#38bdf8',
  muted: '#34413d',
};

export function chartTheme(darkMode: boolean): ChartTheme {
  return darkMode ? DARK_THEME : LIGHT_THEME;
}

export function moneyTooltip(theme: ChartTheme): TooltipConfig {
  return {
    backgroundColor: theme.surface,
    titleColor: theme.text,
    bodyColor: theme.text,
    borderColor: theme.grid,
    borderWidth: 1,
    padding: 12,
    cornerRadius: 12,
    boxPadding: 6,
    usePointStyle: true,
    callbacks: {
      label: (context) => ` ${context.dataset.label ?? context.label}: ${formatMoney(Number(context.raw))}`,
    },
  };
}
