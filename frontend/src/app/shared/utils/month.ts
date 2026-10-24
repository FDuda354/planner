import {addMonths, format, getDaysInMonth, parse} from 'date-fns';
import {pl} from 'date-fns/locale';

const MONTH_KEY_FORMAT = 'yyyy-MM';
const DATE_KEY_FORMAT = 'yyyy-MM-dd';
const MONTH_KEY_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;
const MONTHS_IN_LOCATIVE = [
  'w styczniu', 'w lutym', 'w marcu', 'w kwietniu', 'w maju', 'w czerwcu',
  'w lipcu', 'w sierpniu', 'we wrześniu', 'w październiku', 'w listopadzie', 'w grudniu',
];

export function toMonthKey(date: Date): string {
  return format(date, MONTH_KEY_FORMAT);
}

export function toDateKey(date: Date): string {
  return format(date, DATE_KEY_FORMAT);
}

export function isMonthKey(text: string): boolean {
  return MONTH_KEY_PATTERN.test(text);
}

export function parseMonthKey(monthKey: string): Date {
  return parse(monthKey, MONTH_KEY_FORMAT, new Date());
}

export function parseDateKey(dateKey: string): Date {
  return parse(dateKey, DATE_KEY_FORMAT, new Date());
}

export function shiftMonth(monthKey: string, months: number): string {
  return toMonthKey(addMonths(parseMonthKey(monthKey), months));
}

export function yearOfMonth(monthKey: string): number {
  return Number(monthKey.substring(0, 4));
}

export function monthsOfYear(year: number): string[] {
  return Array.from({length: 12}, (_, index) => `${year}-${String(index + 1).padStart(2, '0')}`);
}

export function daysInMonth(monthKey: string): number {
  return getDaysInMonth(parseMonthKey(monthKey));
}

export function monthLabel(monthKey: string): string {
  return capitalize(format(parseMonthKey(monthKey), 'LLLL yyyy', {locale: pl}));
}

export function monthLabelInGenitive(monthKey: string): string {
  return format(parseMonthKey(monthKey), 'MMMM yyyy', {locale: pl});
}

export function monthNameInLocative(monthKey: string): string {
  return MONTHS_IN_LOCATIVE[parseMonthKey(monthKey).getMonth()];
}

export function monthName(monthKey: string): string {
  return capitalize(format(parseMonthKey(monthKey), 'LLLL', {locale: pl}));
}

export function monthShortName(monthKey: string): string {
  return capitalize(format(parseMonthKey(monthKey), 'LLL', {locale: pl}));
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
