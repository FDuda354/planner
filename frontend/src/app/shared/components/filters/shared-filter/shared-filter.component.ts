import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';

interface SharedFilterOption {
  shared: boolean;
  label: string;
  icon: string;
}

const SHARED_FILTER_OPTIONS: SharedFilterOption[] = [
  {shared: true, label: 'Dobro wspólne', icon: 'pi pi-home'},
  {shared: false, label: 'Osobiste', icon: 'pi pi-user'},
];

@Component({
  selector: 'ui-shared-filter',
  templateUrl: './shared-filter.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedFilterComponent {
  readonly value = input.required<boolean | null>();
  readonly changed = output<boolean | null>();

  readonly options = SHARED_FILTER_OPTIONS;

  toggle(option: SharedFilterOption): void {
    this.changed.emit(this.value() === option.shared ? null : option.shared);
  }
}
