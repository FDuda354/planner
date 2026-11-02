import {formatMoney} from './money';

export function transactionLabel(itemName: string | undefined, amount: number): string {
  return `${itemName ?? ''} · ${formatMoney(amount)}`;
}
