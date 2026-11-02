import {addMonths, addYears, format, getDaysInMonth, isBefore, setDate, setMonth, startOfMonth} from 'date-fns';
import {pl} from 'date-fns/locale';
import {RecurrenceFrequency} from '../../../../models/RecurringTransaction';

export const SHORTEST_MONTH_LENGTH = 28;
const MAX_DAYS_IN_MONTH = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

export interface MonthOption {
  value: number;
  label: string;
}

export const MONTH_OPTIONS: MonthOption[] = Array.from({length: 12}, (_, index) => ({
  value: index + 1,
  label: capitalize(format(new Date(2000, index, 1), 'LLLL', {locale: pl})),
}));

export function maxDayOf(monthOfYear: number | null): number {
  return monthOfYear === null ? 31 : MAX_DAYS_IN_MONTH[monthOfYear - 1];
}

export interface RecurrenceSchedule {
  frequency: RecurrenceFrequency;
  dayOfMonth: number;
  monthOfYear: number | null;
}

export function nextOccurrenceFrom(schedule: RecurrenceSchedule, today: Date, scheduled: Date | null): Date {
  const reference = scheduled === null || isBefore(scheduled, today) ? today : scheduled;
  const candidate = occurrenceInPeriodOf(schedule, reference);
  return isBefore(candidate, today) ? occurrenceAfter(schedule, candidate) : candidate;
}

export function annualDateOf(monthOfYear: number, dayOfMonth: number, fromYear: number): Date {
  let month = new Date(fromYear, monthOfYear - 1, 1);
  while (getDaysInMonth(month) < dayOfMonth) {
    month = addYears(month, 1);
  }
  return setDate(month, dayOfMonth);
}

function occurrenceAfter(schedule: RecurrenceSchedule, occurrence: Date): Date {
  const shifted = schedule.frequency === RecurrenceFrequency.MONTHLY ? addMonths(occurrence, 1) : addYears(occurrence, 1);
  return occurrenceInPeriodOf(schedule, shifted);
}

function occurrenceInPeriodOf(schedule: RecurrenceSchedule, date: Date): Date {
  const month = schedule.frequency === RecurrenceFrequency.MONTHLY
    ? startOfMonth(date)
    : setMonth(startOfMonth(date), schedule.monthOfYear! - 1);
  return setDate(month, Math.min(schedule.dayOfMonth, getDaysInMonth(month)));
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
