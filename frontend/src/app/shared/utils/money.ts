export type MoneyPrecision = 'full' | 'whole';

const FULL_FORMAT = new Intl.NumberFormat('pl-PL', {style: 'currency', currency: 'PLN', useGrouping: true, minimumFractionDigits: 2, maximumFractionDigits: 2});
const WHOLE_FORMAT = new Intl.NumberFormat('pl-PL', {style: 'currency', currency: 'PLN', useGrouping: true, maximumFractionDigits: 0});

export function formatMoney(value: number, precision: MoneyPrecision = 'full'): string {
  return (precision === 'whole' ? WHOLE_FORMAT : FULL_FORMAT).format(value);
}

export function percentOf(part: number, whole: number): number {
  return whole === 0 ? 0 : part / whole;
}
