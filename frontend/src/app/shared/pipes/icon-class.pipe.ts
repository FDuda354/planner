import {Pipe, PipeTransform} from '@angular/core';

const TABLER_PREFIX = 'ti-';

@Pipe({
  name: 'iconClass',
  standalone: false,
})
export class IconClassPipe implements PipeTransform {
  transform(icon: string): string {
    return icon.startsWith(TABLER_PREFIX) ? `ti ${icon}` : `pi ${icon}`;
  }
}
