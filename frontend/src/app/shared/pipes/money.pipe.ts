import {Pipe, PipeTransform} from '@angular/core';
import {formatMoney, MoneyPrecision} from '../utils/money';

@Pipe({
  name: 'money',
  standalone: false,
})
export class MoneyPipe implements PipeTransform {
  transform(value: number, precision: MoneyPrecision = 'full'): string {
    return formatMoney(value, precision);
  }
}
